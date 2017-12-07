import sbt.Keys._
import sbt._

/**
  * This is an attempt to emulate Maven-like multi-project build logic to avoid duplicating build files.
  *
  * If a subproject declares a libraryDependency which corresponds to another subproject's ID, replace the
  * libraryDependency with a source dependency instead.
  *
  * This allows having a standalone library repository that is fully buildable and publishable on its own
  * (via binary dependencies), and including the same repository as e.g. a git submodule as part of a larger
  * project without having to go through a Maven or Ivy repository when iterating on the library in the
  * context of the large project.
  */
object ResolveInternalDependencies {

  def resolveInternalDependenciesImpl(state: State): State = {

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