/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore.shared;

public interface SurveySchemeReference {
	public static interface Visitor<R> {
		R visitDefault();
		R visitYoungScot();
		R visitUclJan15();
		R visitSHeSJun15();
		R visitCrowdflowerNov15();
	}
	
	public static interface SideEffectVisitor {
		void visitDefault();
		void visitYoungScot();
		void visitUclJan15();
		void visitSHeSJun15();
		void visitCrowdflowerNov15();
	}
	
	public static class DefaultScheme implements SurveySchemeReference {
		public static final String ID = "default";
		
		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitDefault();
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitDefault();
		}

		@Override
		public String id() {
			return ID;
		}

		@Override
		public String description() {
			return "Default scheme";
		}
	}
	
	public static class YoungScot2014Scheme implements SurveySchemeReference {
		public static final String ID = "young_scot_2014";

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitYoungScot();
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitYoungScot();			
		}

		@Override
		public String id() {
			return "young_scot_2014";
		}

		@Override
		public String description() {
			return "Young Scot 2014"; 
		}
	}
	
	public static class UclJan15Scheme implements SurveySchemeReference {
		public static final String ID = "ucl_jan_2015";

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitUclJan15();
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitUclJan15();			
		}

		@Override
		public String id() {
			return ID;
		}

		@Override
		public String description() {
			return "University College London January 2015"; 
		}
	}
	
	public static class SHeSJun15Scheme implements SurveySchemeReference {
		public static final String ID = "SHeS_jun_2015";

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitSHeSJun15();
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitSHeSJun15();	
		}

		@Override
		public String id() {
			return ID;
		}

		@Override
		public String description() {
			return "SHeS Jun 2015"; 
		}		
	}
	
	public static class CrowdflowerNov15Scheme implements SurveySchemeReference {
		public static final String ID = "crowdflower_nov_2015";

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitCrowdflowerNov15();
		}

		@Override
		public void accept(SideEffectVisitor visitor) {
			visitor.visitCrowdflowerNov15();			
		}

		@Override
		public String id() {
			return ID;
		}

		@Override
		public String description() {
			return "Crowdflower Test Nov 2015";
		}		
	}
		
	public <R> R accept(Visitor<R> visitor);
	public void accept (SideEffectVisitor visitor);
	public String id();
	public String description();
}