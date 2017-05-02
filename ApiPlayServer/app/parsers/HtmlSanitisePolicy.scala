package parsers

import org.owasp.html.{AttributePolicy, HtmlPolicyBuilder, PolicyFactory, Sanitizers}

import scala.util.Try

/**
  * Created by Tim Osadchiy on 27/04/2017.
  */

private class HtmlSanitisePolicy {

}

object HtmlSanitisePolicy {

  private val WEB_UNIT_POLICY = new AttributePolicy {
    /** Copy of org.owasp.html.Sanitizers private INTEGER policy **/
    def apply(elementName: String, attributeName: String, value: String): String = {
      val d = value.replaceAll("""(px|%|em|rem)$""", "")
      if (Try(d.toDouble).isSuccess) {
        value
      } else {
        null
      }
    }
  }

  private val YOUTUBE_URL_POLICY = new AttributePolicy {
    def apply(elementName: String, attributeName: String, value: String) = {
      val Pattern = """^(https?\:\/\/)?(www\.)?(youtube\.com|youtu\.?be)\/.+$"""
      if (value.matches(Pattern)) {
        value
      } else {
        null
      }
    }
  }

  private val IFRAME_POLICY: PolicyFactory = new HtmlPolicyBuilder()
    .allowUrlProtocols("http", "https")
    .allowElements("iframe")
    .allowAttributes("frameborder", "allowfullscreen")
    .onElements("iframe")
    .allowAttributes("width", "height", "frameborder")
    .matching(WEB_UNIT_POLICY)
    .onElements("iframe")
    .allowAttributes("src")
    .matching(YOUTUBE_URL_POLICY)
    .onElements("iframe").toFactory

  def sanitise(html: String): String = {
    val policy: PolicyFactory = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS)
      .and(Sanitizers.LINKS)
    policy.sanitize(html)
  }

  def easedSanitise(html: String) = {
    val policy: PolicyFactory = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS)
      .and(Sanitizers.LINKS).and(Sanitizers.IMAGES).and(IFRAME_POLICY)
    policy.sanitize(html)
  }

}
