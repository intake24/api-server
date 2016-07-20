/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.DrinkwareSet

case class PortionResources (asServedSets: Seq[AsServedSet], guideImages: Seq[GuideImage], drinkwareSets: Seq[DrinkwareSet])