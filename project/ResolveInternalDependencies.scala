package sbt.intake24

import sbt._
import Keys._
import sbt.internal.Load

object ResolveInternalDependencies {

  def resolveInternalDependencies = Command.command("resolveInternalDependencies") {
    state =>

      val projectState = Project.extract(state)

      val allProjectRefs = projectState.structure.allProjectRefs

      val projectIdMap = allProjectRefs.foldLeft(Map[ModuleID, ProjectRef]()) {
        case (acc, ref) =>
          projectID.in(ref).get(projectState.structure.data) match {
            case Some(id) =>
              acc + (id.withExplicitArtifacts(Vector()) -> ref)
            case None => acc
          }
      }

      val newSettings = allProjectRefs.foldLeft(Seq[Setting[_]]()) {
        (settings, projectRef) =>
          val libDepKey = libraryDependencies.in(projectRef)
          libDepKey.get(projectState.structure.data) match {
            case Some(deps) =>
              deps.foldLeft(settings) {
                (settings, libId) =>
                  projectIdMap.get(libId) match {
                    case Some(replacementRef) =>

                      println(s"Replacing " + libId + " in " + projectRef + " with " + replacementRef)

                      val newDeps = deps.filterNot(_ == libId)

                      //BuildStru

                      //state.u

                      //println(s"Original libraryDependencies: $deps")
                      //println(s"New libraryDependencies: $newDeps")

                      //projectState.structure.data.

                      settings ++ Seq(libDepKey -= libId, buildDependencies.in(Global) ~= (_.addClasspath(projectRef, ResolvedClasspathDependency(replacementRef, None))))

                    //projectState.append(libDepKey -= libId, state)

                    //println(state.get(libDepKey.key).map(_.mkString("\n")))

                    //val stateMinusLib = pro
                    //state.put(libDepKey.key, newDeps)

                    /*stateMinusLib.update(buildDependencies.in(Global).key) {
                    case Some(originalDeps) =>
                      val newDeps = originalDeps.addClasspath(projectRef, ClasspathDependency(replacementRef, None))
                    case None => throw new IllegalStateException("Should not be here")
                  }*/

                    //Load.reapppl
                    case None =>
                      settings
                  }
              }

            case None => settings
          }
      }
      //val xfsettings = Load.transformSettings(Load.projectScope(projectState.currentRef), projectState.currentRef.build, projectState.rootProject, newSettings)
      //state.put(stateBuildStructure, Load.reapply(projectState.session.original ++ xfsettings, projectState.structure)(Def.showRelativeKey(projectState.session.current, projectState.structure.allProjects.size > 1, None)))
      projectState.append(newSettings, state)
  }
}