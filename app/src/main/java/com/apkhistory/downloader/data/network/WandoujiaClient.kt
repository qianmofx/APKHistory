package com.apkhistory.downloader.data.network

import com.apkhistory.downloader.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 豌豆荚 HTML 解析客户端。
 * 所有数据通过解析 SSR 页面 + data-* 属性获取。
 */
class WandoujiaClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", UA)
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .build()
            chain.proceed(req)
        }
        .build()

    companion object {
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        const val BASE_URL = "https://www.wandoujia.com"
    }

    // =============== 搜索 ===============

    suspend fun searchApps(keyword: String, page: Int = 1): List<SearchResult> = withContext(Dispatchers.IO) {
        val doc = fetch("$BASE_URL/search?key=${URLEncoder.encode(keyword, "UTF-8")}&page=$page")
        doc.select("li.search-item").map { item ->
            val link = item.select("a.detail-check-btn").first()
            SearchResult(
                appId = link?.attr("data-app-id") ?: "",
                name = item.select(".name").text().ifEmpty { item.select(".app-title-h2 a").text() },
                packageName = link?.attr("data-app-pname") ?: "",
                vid = link?.attr("data-app-vid") ?: "",
                versionName = link?.attr("data-app-vname") ?: "",
                versionCode = link?.attr("data-app-vcode") ?: "",
                iconUrl = link?.attr("data-app-icon").orEmpty().ifEmpty {
                    item.select(".icon-wrap img").attr("src")
                },
                installCount = item.select(".meta span").text(),
                description = item.select(".comment").text().take(200),
                detailUrl = link?.attr("href") ?: ""
            )
        }
    }

    // =============== 应用详情 ===============

    suspend fun getAppDetail(appId: String): AppDetail = withContext(Dispatchers.IO) {
        val doc = fetch("$BASE_URL/apps/$appId")
        val body = doc.body()
        val infos = doc.select("dl.infos-list")
        val name = doc.select(".app-info .title").text().ifEmpty { body.attr("data-title") }

        // 截图——取原图 data-src 而不是反推缩略图 url
        val screenshots = doc.select(".screen-list-img").eachAttr("data-src")
            .filter { it.isNotEmpty() }
            .map { if (it.startsWith("//")) "https:$it" else it }

        // 更新内容
        val changelogDiv = doc.select(".desc-info .con div")
        val changelog = if (changelogDiv.size >= 2) changelogDiv[1].html().replace("<br>", "\n") else ""

        // 标签
        val tags = doc.select(".tag-box a[href*=/tag/]").eachText()

        AppDetail(
            appId = appId,
            name = name,
            packageName = body.attr("data-pn"),
            currentVid = body.attr("data-app-vid"),
            // data-app-vname/vcode 不在 <body> 上，在下载按钮 <a> 上
            currentVersionName = body.attr("data-app-vname").ifEmpty {
                doc.select("a[data-app-vname]").first()?.attr("data-app-vname") ?: ""
            },
            currentVersionCode = body.attr("data-app-vcode").ifEmpty {
                doc.select("a[data-app-vname]").first()?.attr("data-app-vcode") ?: ""
            },
            iconUrl = doc.select(".app-icon img").attr("src"),
            // itemprop="fileSize" 是大小字段独有的 meta 标签
            size = doc.select("meta[itemprop=fileSize]").attr("content").ifEmpty {
                // 兜底：部分页面 meta 在 script 模板中，用 Jsoup 重解析
                Jsoup.parse(doc.html()).select("meta[itemprop=fileSize]").attr("content").ifEmpty {
                    Regex("""itemprop="fileSize"\s+content="([^"]+)"""")
                        .find(doc.html())?.groupValues?.getOrNull(1) ?: ""
                }
            },
            updateDate = infos.select("[datetime]").attr("datetime"),
            systemRequirement = infos.select("dd.perms").text().substringBefore("敏感"),
            category = infos.select(".tag-box a").first()?.text() ?: "",
            subCategory = infos.select(".tag-box a").getOrNull(1)?.text() ?: "",
            developer = infos.select("[itemprop=name]").text().ifEmpty {
                // 兜底：从 infos 列表中找可能的开发者 dd 项
                infos.select("dd").firstOrNull { it.text().contains("公司") || it.text().contains("工作室") }?.text() ?: ""
            },
            installCount = doc.select("[itemprop=interactionCount]").attr("content")
                .removePrefix("UserDownloads:")
                .let { if (it.isEmpty() || it == "content") doc.select(".install i").text() else it },
            rating = doc.select(".love i").text(),
            description = doc.select(".desc-info .con div").first()?.text() ?: "",
            changelog = changelog,
            screenshotUrls = screenshots,
            tags = tags,
            contentRating = doc.select(".update-time").text().let {
                if (it.contains("12周岁")) "年满12周岁" else it
            },
            privacyPolicyUrl = doc.select("a.privacy-link").attr("href")
        )
    }

    // =============== 历史版本列表 ===============

    suspend fun getVersions(appId: String): List<AppVersion> = withContext(Dispatchers.IO) {
        val doc = fetch("$BASE_URL/apps/$appId/history")
        val items = doc.select("ul.old-version-list > li")
        items.mapNotNull { item ->
            // 优先从 <li> 的 data-* 属性取，没有则从子元素 <a> 取
            val vid = item.attr("data-app-vid").ifEmpty {
                item.select("a[data-app-vid]").first()?.attr("data-app-vid") ?: return@mapNotNull null
            }
            val versionCode = item.attr("data-app-vcode").ifEmpty {
                item.select("a[data-app-vcode]").first()?.attr("data-app-vcode") ?: ""
            }
            val versionName = item.attr("data-app-vname").ifEmpty {
                item.select("a[data-app-vname]").first()?.attr("data-app-vname") ?: ""
            }
            val iconUrl = item.attr("data-app-icon").ifEmpty {
                item.select("a[data-app-icon]").first()?.attr("data-app-icon") ?: ""
            }
            val detailLink = item.select("> a").first()
            val apkSize = detailLink?.select("span")?.text() ?: ""

            AppVersion(appId = appId, vid = vid, versionName = versionName,
                versionCode = versionCode, iconUrl = iconUrl, apkSize = apkSize)
        }
    }

    // =============== 版本详情（含 changelog + 发布日期） ===============

    suspend fun getVersionDetail(appId: String, vcode: String): VersionDetail = withContext(Dispatchers.IO) {
        val doc = fetch("$BASE_URL/apps/$appId/history_v$vcode")
        val infos = doc.select("dl.infos-list")
        val changelog = doc.select(".history-desc .con div").html()
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .trim()

        val permissions = doc.select("#j-perms-list .perms")
            .eachText()
            .filter { it.length < 50 }

        // 从版本详情页提取 APK 直链（核心！不走 /download?vid= 拼接）
        val pageDownloadUrl = parseDownloadUrl(doc)
        // 解析应用图标
        val pageIconUrl = doc.select(".icon-wrap img").attr("src").let { src ->
            if (src.startsWith("//")) "https:$src" else src
        }

        VersionDetail(
            vid = doc.body().attr("data-app-vid"),
            vcode = vcode,
            versionName = doc.select(".version-name").text()
                .replaceFirst("^版本(号)?[：:]?\\s*".toRegex(), "")
                .trim()
                .ifEmpty {
                doc.body().attr("data-app-vname").ifEmpty {
                    Regex("""v(\d+\.\d+\.\d+(?:\.\d+)?)""").find(doc.title())?.groupValues?.getOrNull(1)
                        ?: "未知"
                }
            },
            appName = doc.body().attr("data-title").ifEmpty {
                val title = doc.title()
                Regex("""(\d+)(.+?)v\d""").find(title)?.groupValues?.getOrNull(2)
                    ?: title.substringBefore("_")
            },
            apkSize = doc.select("meta[itemprop=fileSize]").attr("content").ifEmpty {
                Jsoup.parse(doc.html()).select("meta[itemprop=fileSize]").attr("content")
            },
            updateDate = doc.select(".update-time").text().replace("更新时间：", ""),
            systemRequirement = infos.select("dd.perms").text().substringBefore("敏感"),
            permissions = permissions,
            changelog = changelog.ifEmpty { "本次更新:\n优化了一些已知的问题。" },
            developer = infos.select("[itemprop=name]").text(),
            category = infos.select(".tag-box a").first()?.text() ?: "",
            downloadUrl = pageDownloadUrl,
            iconUrl = pageIconUrl
        )
    }

    /**
     * 从版本详情页提取 APK 直链下载地址。
     *
     * 豌豆荚的下载机制：
     * - 下载按钮有 class "normal-dl-btn"，其 data-href 属性就是 CDN 直链
     * - 备选：a[data-type='history'] 的 href 是绑定跳转链接
     *
     * 按文档推荐优先级：
     * 1. .normal-dl-btn → data-href（CDN 直链）
     * 2. a[data-type='history'] → href（绑定跳转）
     * 3. 正则 data-href 兜底
     */
    private fun parseDownloadUrl(doc: Document): String {
        // 1. 直链方式（推荐）：.normal-dl-btn → data-href
        val directUrl = doc.select(".normal-dl-btn").attr("data-href")
        if (directUrl.isNotEmpty()) return directUrl

        // 2. 绑定跳转方式：a[data-type='history'] → href
        val bindingUrl = doc.select("a[data-type='history']").attr("href")
        if (bindingUrl.isNotEmpty()) {
            return if (bindingUrl.startsWith("//")) "https:$bindingUrl"
            else if (bindingUrl.startsWith("/")) "$BASE_URL$bindingUrl"
            else bindingUrl
        }

        // 3. 正则兜底：搜索整个 HTML 中的 data-href
        val html = doc.html()
        val regex = Regex("""data-href="(https?://[^"]+)"""")
        val match = regex.find(html)
        if (match != null) return match.groupValues[1]

        return ""
    }

    /**
     * 构造最新版下载 URL（仅用于最新版下载）。
     * 历史版本不走此方法，从版本详情页的 data-href 提取直链。
     */
    fun getDownloadUrl(appId: String, vid: String): String {
        return "$BASE_URL/apps/$appId/download?vid=$vid"
    }

    private fun fetch(url: String): Document {
        val request = Request.Builder().url(url)
            .header("User-Agent", UA)
            .build()
        return client.newCall(request).execute().use { response ->
            val html = response.body?.string() ?: throw Exception("Empty response")
            Jsoup.parse(html)
        }
    }
}
