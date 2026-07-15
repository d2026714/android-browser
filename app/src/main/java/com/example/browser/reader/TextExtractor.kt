package com.example.browser.reader

import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume

object TextExtractor {

    data class ExtractedContent(val title: String, val text: String, val chapters: List<Chapter>)
    data class Chapter(val title: String, val content: String)

    private const val JS = """
    (function(){
        document.querySelectorAll('script,style,nav,header,footer,aside,iframe,.ad,.sidebar,.menu').forEach(e=>e.remove());
        var blocks=[], els=document.querySelectorAll('p,div,article,section');
        for(var i=0;i<els.length;i++){
            var t=els[i].innerText.trim();
            if(t.length<20)continue;
            var lk=0;els[i].querySelectorAll('a').forEach(a=>lk+=a.innerText.length);
            var d=t.length>0?(t.length-lk)/t.length:0;
            if(d<0.4)continue;
            blocks.push({text:t,score:t.length*d});
        }
        blocks.sort((a,b)=>b.score-a.score);
        var top=blocks.slice(0,50);
        var result=top.map(b=>b.text).join('\n\n');
        var chapters=[], lines=result.split('\n'), cur={t:'',c:''};
        for(var k=0;k<lines.length;k++){
            var l=lines[k].trim();
            if(!l)continue;
            if(/^(第[一二三四五六七八九十百千零〇0-9]+[章节回卷集部篇]|Chapter\s+\d+)/i.test(l)){
                if(cur.t||cur.c)chapters.push({title:cur.t||'未命名',content:cur.c.trim()});
                cur={t:l,c:''};
            }else cur.c+=l+'\n';
        }
        if(cur.t||cur.c)chapters.push({title:cur.t||'未命名',content:cur.c.trim()});
        return JSON.stringify({title:document.title,text:result,chapters:chapters});
    })()"""

    suspend fun extract(wv: WebView): ExtractedContent = withContext(Dispatchers.Main) {
        val json = suspendCancellableCoroutine { cont ->
            wv.evaluateJavascript(JS) { cont.resume(it ?: "{}") }
        }
        parse(json, wv.title ?: "")
    }

    private val noisePatterns = listOf(
        "本章说", "同人创作", "评论区", "书友圈", "上起点App", "上起点", "查看全部",
        "推荐阅读", "猜你喜欢", "相关推荐", "热门推荐", "最新章节",
        "广告", "加入书架", "投推荐票", "月票", "打赏", "催更",
    )

    private fun parse(json: String, fallback: String): ExtractedContent = try {
        val clean = json.removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n")
        val obj = JSONObject(clean)
        val title = obj.optString("title", fallback)
        val text = obj.optString("text", "")
        val arr = obj.optJSONArray("chapters")
        val ch = if (arr != null) {
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it); Chapter(o.getString("title"), o.getString("content"))
            }.filter { c ->
                // Filter out noise content
                val lower = c.content.lowercase()
                noisePatterns.none { noise -> lower.contains(noise.lowercase()) }
            }.distinctBy { it.title } // Dedup by title
        } else emptyList()
        ExtractedContent(title, text, ch)
    } catch (_: Exception) { ExtractedContent(fallback, "", emptyList()) }
}
