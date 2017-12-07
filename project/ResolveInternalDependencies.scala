package sbt.intake24

import sbt._
import Keys._

object ResolveInternalDependencies {

  def resolveInternalDependenciesImpl(state: State): State = {

    val projectState = Project.extract(state)

    state.log.info(state.remainingCommands.map(_.commandLine).mkString(", "))

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
                    state.log.info(s"Replacing binary dependency $libId in ${projectRef.project} with source dependency ${replacementRef.project}")
                    settings ++ Seq(libDepKey -= libId, buildDependencies.in(Global) ~= (_.addClasspath(projectRef, ResolvedClasspathDependency(replacementRef, None))))

                  case None =>
                    settings
                }
            }

          case None => settings
        }
    }

    if (newSettings.nonEmpty)
      projectState.append(newSettings, state)
    else
      state
  }

  def resolveInternalDependencies = Command.command("resolveInternalDependencies")(resolveInternalDependenciesImpl)
}