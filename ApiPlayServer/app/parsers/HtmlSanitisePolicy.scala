package parsers

import org.owasp.html.{PolicyFactory, Sanitizers}

/**
  * Created by Tim Osadchiy on 27/04/2017.
  */
object HtmlSanitisePolicy {

  def sanitise(html: String): String = {
    val policy: PolicyFactory = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS)
      .and(Sanitizers.LINKS).and(Sanitizers.IMAGES).and(Sanitizers.STYLES)
    policy.sanitize(html)
  }

}
