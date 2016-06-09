package uk.ac.ncl.openlab.intake24.admin.views

import scalatags.JsDom._
import org.scalajs.dom.html._
import implicits._
import attrs._
import tags._
import tags2._
import org.scalajs.dom.html

object SBAdmin {

  import attrs.ExtendedString

  val dataToggle = "data-toggle".attr
  val dataTarget = "data-target".attr

  def dropdown() = {

  }

  def sidebar() =
    div(cls := "collapse navbar-collapse navbar-ex1-collapse")(
      ul(cls := "nav navbar-nav side-nav")(
        li()(a(href := "#")(i(cls := "fa fa-fw fa-dashboard"), "Dashboard")),
        li()(a(href := "javascript:;", dataToggle := "collapse", dataTarget := "#demo")(i(cls := "fa fa-fw fa-arrows-v"), "Dropdown", i(cls := "fa fa-fw fa-caret-down")),
          ul(id := "demo", cls := "collapse")(
            li(a(href := "#")("Dropdown Item 1")),
            li(a(href := "#")("Dropdown Item 2"))))))

  def navigation() = {
    div(id := "wrapper")(
      nav(cls := "navbar navbar-inverse navbar-fixed-top")(
        div(cls := "navbar-header")(
          button(`type` := "button", cls := "navbar-toggle", dataToggle := "collapse", dataTarget := ".navbar-ex1-collapse")(
            span(cls := "sr-only")("Toggle navigation"),
            span(cls := "icon-bar"),
            span(cls := "icon-bar"),
            span(cls := "icon-bar")),
          a(cls := "navbar-brand", href := "index.html")("Intake24 Admin")),
        ul(cls := "nav navbar-right top-nav")(
          li(cls := "dropdown")(
            a(href := "#", cls := "dropdown-toggle", dataToggle := "dropdown")(i(cls := "fa fa-envelope"), b(cls := "caret")),
            ul(cls := "dropdown-menu message-dropdown")(
              li(cls := "message-preview")(
                a(href := "#")(
                  div(cls := "media")(
                    span(cls := "pull-left")(
                      img(cls := "media-object", src := "http://placehold.it/50x50", alt := "")),
                    div(cls := "media-body")(
                      h5(cls := "media-heading")(strong()("John Smith")),
                      p(cls := "small text-muted")(i(cls := "fa fa-clock-o")("Yesterday at 4:32 PM")),
                      p()("Lorem ipsum dolor sit amet, consectetur..."))))),
              li(cls := "message-footer")(a(href:="#")("Read all messages")))),
          li(cls := "dropdown")(
            a(href := "#", cls := "dropdown-toggle", dataToggle := "dropdown")(
              (i(cls := "fa fa-bell")),
              (b(cls := "caret"))),
            ul(cls := "dropdown-menu alert-dropdown")(
              li()(a(href := "#")("Alert Name", span(cls := "label label-default")("Alert Badge")))))),
        sidebar()))
  }

  /* 
   *         <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <!-- Brand and toggle get grouped for better mobile display -->
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-ex1-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="index.html">SB Admin</a>
            </div>
            <!-- Top Menu Items -->
            <ul class="nav navbar-right top-nav">
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-envelope"></i> <b class="caret"></b></a>
                    <ul class="dropdown-menu message-dropdown">
                        <li class="message-preview">
                            <a href="#">
                                <div class="media">
                                    <span class="pull-left">
                                        <img class="media-object" src="http://placehold.it/50x50" alt="">
                                    </span>
                                    <div class="media-body">
                                        <h5 class="media-heading"><strong>John Smith</strong>
                                        </h5>
                                        <p class="small text-muted"><i class="fa fa-clock-o"></i> Yesterday at 4:32 PM</p>
                                        <p>Lorem ipsum dolor sit amet, consectetur...</p>
                                    </div>
                                </div>
                            </a>
                        </li>
                        <li class="message-preview">
                            <a href="#">
                                <div class="media">
                                    <span class="pull-left">
                                        <img class="media-object" src="http://placehold.it/50x50" alt="">
                                    </span>
                                    <div class="media-body">
                                        <h5 class="media-heading"><strong>John Smith</strong>
                                        </h5>
                                        <p class="small text-muted"><i class="fa fa-clock-o"></i> Yesterday at 4:32 PM</p>
                                        <p>Lorem ipsum dolor sit amet, consectetur...</p>
                                    </div>
                                </div>
                            </a>
                        </li>
                        <li class="message-preview">
                            <a href="#">
                                <div class="media">
                                    <span class="pull-left">
                                        <img class="media-object" src="http://placehold.it/50x50" alt="">
                                    </span>
                                    <div class="media-body">
                                        <h5 class="media-heading"><strong>John Smith</strong>
                                        </h5>
                                        <p class="small text-muted"><i class="fa fa-clock-o"></i> Yesterday at 4:32 PM</p>
                                        <p>Lorem ipsum dolor sit amet, consectetur...</p>
                                    </div>
                                </div>
                            </a>
                        </li>
           
                    </ul>
                </li>
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-bell"></i> <b class="caret"></b></a>
                    <ul class="dropdown-menu alert-dropdown">
                        <li>
                            <a href="#">Alert Name <span class="label label-default">Alert Badge</span></a>
                        </li>
                        <li>
                            <a href="#">Alert Name <span class="label label-primary">Alert Badge</span></a>
                        </li>
                        <li>
                            <a href="#">Alert Name <span class="label label-success">Alert Badge</span></a>
                        </li>
                        <li>
                            <a href="#">Alert Name <span class="label label-info">Alert Badge</span></a>
                        </li>
                        <li>
                            <a href="#">Alert Name <span class="label label-warning">Alert Badge</span></a>
                        </li>
                        <li>
                            <a href="#">Alert Name <span class="label label-danger">Alert Badge</span></a>
                        </li>
                        <li class="divider"></li>
                        <li>
                            <a href="#">View All</a>
                        </li>
                    </ul>
                </li>
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-user"></i> John Smith <b class="caret"></b></a>
                    <ul class="dropdown-menu">
                        <li>
                            <a href="#"><i class="fa fa-fw fa-user"></i> Profile</a>
                        </li>
                        <li>
                            <a href="#"><i class="fa fa-fw fa-envelope"></i> Inbox</a>
                        </li>
                        <li>
                            <a href="#"><i class="fa fa-fw fa-gear"></i> Settings</a>
                        </li>
                        <li class="divider"></li>
                        <li>
                            <a href="#"><i class="fa fa-fw fa-power-off"></i> Log Out</a>
                        </li>
                    </ul>
                </li>
            </ul>
            <!-- Sidebar Menu Items - These collapse to the responsive navigation menu on small screens -->
            
            <!-- /.navbar-collapse -->
        </nav>
   * 
   */

}