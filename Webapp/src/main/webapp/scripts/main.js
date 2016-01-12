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

// Main scripts

var panelShowing = false;

$(document).ready(function() {

	$('body').prepend('<img id="bg" class="bgwidth" src="https://intake24.co.uk/images/background.jpg" />');

});

$(window).resize(function() {

	doResize();

});

function doResize() {
	var windowHeight = $(window).height();
	var headerHeight = $("#header-container").outerHeight(true);
	var navbarHeight = $("#navigation-bar-container").outerHeight(true);
	var footerHeight = $("#footer-container").outerHeight(true);

	var minHeight = windowHeight - headerHeight - navbarHeight - footerHeight;
	var panelHeight = $('#intake24-meals-panel .intake24-meal-list').height() + 100;

	if (minHeight < panelHeight) {
		minHeight = panelHeight;
	};
	// Content height
	$("#main-content").css("min-height", minHeight);

	// Side panel
	$('#intake24-meals-panel').css('height', minHeight);

	// Background image
	var aspectRatio = 1236 / 984;

	if ( ($(window).width() / $(window).height()) < aspectRatio ) {
		$('#bg').removeClass().addClass('bgheight');
	} else {
		$('#bg').removeClass().addClass('bgwidth');
	}

	// Check panel position
	if ($('body').width() >= '960') {
		$("#intake24-meals-panel").css('margin-left', '0px');
		$(".intake24-meals-panel-header-button").removeClass('button-show');
		$(".intake24-meals-panel-header-button").removeClass('button-hide');
		$(".intake24-meals-panel-header-button").addClass('button-menu');
	} else {
		if (!panelShowing)
			$("#intake24-meals-panel").css('margin-left', '-230px');
		$(".intake24-meals-panel-header-button").removeClass('button-menu');
		$(".intake24-meals-panel-header-button").removeClass('button-hide');
		$(".intake24-meals-panel-header-button").addClass('button-show');
	}

	// Other
	// $('.intake24-meals-panel-header').html('Your Food Intake');
	// $('#intake24-meals-panel button').html('+ Add Another Meal');

	bindEvents();
}

function bindEvents() {

	var clickHandler = ('ontouchstart' in document.documentElement ? "touchstart" : "click");

	$('.intake24-meals-panel-header-container').unbind(clickHandler).bind(clickHandler,function(e){
		togglePanel();
	});

	$(document).unbind(clickHandler).bind(clickHandler,function(e){

	    var container = $('#intake24-meals-panel');

	    if (!container.is(e.target) && container.has(e.target).length === 0) {
	    	hidePanel();
	    }
	});

}

function showPanel() {

	if ($('body').width() < '940') {
		if (!panelShowing) {
			panelShowing = true;
			$("#intake24-meals-panel").stop().animate({'margin-left':'0px'}, 300);
		$(".intake24-meals-panel-header-button").removeClass('button-show');
		$(".intake24-meals-panel-header-button").removeClass('button-menu');
		$(".intake24-meals-panel-header-button").addClass('button-hide');
		}
	}
	
}

function hidePanel() {

	if ($('body').width() < '940') {
		if (panelShowing) {
			panelShowing = false;
			$("#intake24-meals-panel").stop().delay(200).animate({'margin-left':'-230px'}, 200);
		$(".intake24-meals-panel-header-button").removeClass('button-menu');
		$(".intake24-meals-panel-header-button").removeClass('button-hide');
		$(".intake24-meals-panel-header-button").addClass('button-show');
		}
	}
	
}

function togglePanel() {

	if ($('body').width() < '940') {
		if (panelShowing) {
			panelShowing = false;
			$("#intake24-meals-panel").stop().animate({'margin-left':'-230px'}, 200);
		$(".intake24-meals-panel-header-button").removeClass('button-menu');
		$(".intake24-meals-panel-header-button").removeClass('button-hide');
		$(".intake24-meals-panel-header-button").addClass('button-show');
		} else {
			panelShowing = true;
			$("#intake24-meals-panel").stop().animate({'margin-left':'0px'}, 300);
		$(".intake24-meals-panel-header-button").removeClass('button-show');
		$(".intake24-meals-panel-header-button").removeClass('button-menu');
		$(".intake24-meals-panel-header-button").addClass('button-hide');
		}
	}
}
