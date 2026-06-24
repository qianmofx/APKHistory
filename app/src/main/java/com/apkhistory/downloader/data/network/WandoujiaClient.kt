package com.apkhistory.downloader.data.network

import com.apkhistory.downloader.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
        val doc = fetch("$BASE_URL/search?key=${java.net.URLEncoder.encode(keyword, "UTF-8")}&page=$page")
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
                Regex("""itemprop="fileSize"\s+content="([^"]+)"""")
                    .find(doc.html())?.groupValues?.getOrNull(1) ?: ""
            },
            updateDate = infos.select("[datetime]").attr("datetime"),
            systemRequirement = infos.select("dd.perms").text().substringBefore("敏感"),
            category = infos.select(".tag-box a").first()?.text() ?: "",
            subCategory = if (infos.select(".tag-box a").size > 1) infos.select(".tag-box a")[1].text() else "",
            developer = infos.select("[itemprop=name]").text().ifEmpty {
                infos.select("dd").find { it.text().contains("腾讯") }?.text() ?: ""
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
        val versions = mutableListOf<AppVersion>()

        // 当前版本
        val detailDoc = if (items.isEmpty()) doc else fetch("$BASE_URL/apps/$appId")

        for (item in items) {
            val link = item.select("> a[data-app-vname]").first()
                ?: item.select("a[data-app-vname]").first()
            if (link == null) {
                // 如果是通过 <a href> 包裹图片和文本的结构
                val textLink = item.select("> a").first()
                val detailBtn = item.select("a.detail-check-btn").first()
                if (detailBtn != null) {
                    val nameText = textLink?.select("p")?.text() ?: ""
                    val cleanName = nameText.replace("微信 ", "").trim()
                    versions.add(AppVersion(
                        appId = appId,
                        vid = detailBtn.attr("data-app-vid"),
                        versionName = detailBtn.attr("data-app-vname").ifEmpty { cleanName },
                        versionCode = detailBtn.attr("data-app-vcode"),
                        iconUrl = detailBtn.attr("data-app-icon").ifEmpty {
                            textLink?.select("img")?.attr("src") ?: ""
                        },
                        apkSize = textLink?.select("span")?.text() ?: ""
                    ))
                }
            } else {
                val pText = item.select("> a p").text()
                versions.add(AppVersion(
                    appId = appId,
                    vid = link.attr("data-app-vid"),
                    versionName = link.attr("data-app-vname"),
                    versionCode = link.attr("data-app-vcode"),
                    iconUrl = link.attr("data-app-icon"),
                    apkSize = item.select("> a span").text()
                ))
            }
        }
        versions
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

        VersionDetail(
            vid = doc.body().attr("data-app-vid"),
            vcode = vcode,
            versionName = doc.body().attr("data-app-vname").ifEmpty {
                // 版本详情页 body 没有 data-app-vname，从标题提取
                // 标题格式: "2026微信v8.0.72老旧历史版本..."
                Regex("""v(\d+\.\d+\.\d+(?:\.\d+)?)""").find(doc.title())?.groupValues?.getOrNull(1)
                    ?: "未知"
            },
            appName = doc.body().attr("data-title").ifEmpty {
                // 从标题提取: "2026微信v8.0.72..." → "微信"
                val title = doc.title()
                Regex("""(\d+)(.+?)v\d""").find(title)?.groupValues?.getOrNull(2)
                    ?: title.substringBefore("_")
            },
            apkSize = doc.select("meta[itemprop=fileSize]").attr("content").ifEmpty {
                Regex("""itemprop="fileSize"\s+content="([^"]+)"""")
                    .find(doc.html())?.groupValues?.getOrNull(1) ?: ""
            },
            updateDate = doc.select(".update-time").text().replace("更新时间：", ""),
            systemRequirement = infos.select("dd.perms").text().substringBefore("敏感"),
            permissions = permissions,
            changelog = changelog.ifEmpty { "本次更新:\n优化了一些已知的问题。" },
            developer = infos.select("[itemprop=name]").text(),
            category = infos.select(".tag-box a").first()?.text() ?: ""
        )
    }

    // =============== 类别浏览 ===============

    suspend fun getCategoryApps(categoryId: String, page: Int = 1): List<SearchResult> = withContext(Dispatchers.IO) {
        val doc = fetch("$BASE_URL/category/$categoryId?page=$page")
        doc.select("a.detail-check-btn").mapNotNull { link ->
            val appId = link.attr("data-app-id")
            if (appId.isEmpty()) null else SearchResult(
                appId = appId,
                name = link.attr("data-app-name"),
                packageName = link.attr("data-app-pname"),
                vid = link.attr("data-app-vid"),
                versionName = link.attr("data-app-vname"),
                versionCode = link.attr("data-app-vcode"),
                iconUrl = link.attr("data-app-icon"),
                installCount = "",
                description = "",
                detailUrl = link.attr("href")
            )
        }
    }

    // =============== 排行榜 ===============

    suspend fun getTopApps(type: String = "app"): List<SearchResult> = withContext(Dispatchers.IO) {
        val doc = fetch("$BASE_URL/top/$type")
        doc.select("a.detail-check-btn").mapNotNull { link ->
            val appId = link.attr("data-app-id")
            if (appId.isEmpty()) null else SearchResult(
                appId = appId,
                name = link.attr("data-app-name"),
                packageName = link.attr("data-app-pname"),
                vid = link.attr("data-app-vid"),
                versionName = link.attr("data-app-vname"),
                versionCode = link.attr("data-app-vcode"),
                iconUrl = link.attr("data-app-icon"),
                installCount = "",
                description = "",
                detailUrl = link.attr("href")
            )
        }
    }

    fun getDownloadUrl(appId: String, vid: String): String {
        return "$BASE_URL/apps/$appId/download?vid=$vid"
    }

    private fun fetch(url: String): Document {
        val request = Request.Builder().url(url)
            .header("User-Agent", UA)
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw Exception("Empty response")
        return Jsoup.parse(html)
    }
}
