package com.example.ui.components.map

/**
 * High-definition custom styling JSONs for Google Maps.
 * Ensures prominent visibility of POIs (Hospitals, Police, Schools, Gas Stations, Hotels, Roads).
 */
object GoogleMapStyles {

    /**
     * Professional Cyber Dark Map Style
     * Highlights roads, water, parks, and makes POI icons vibrant and distinct.
     */
    const val CYBER_DARK_JSON = """[
      {
        "elementType": "geometry",
        "stylers": [
          { "color": "#18181b" }
        ]
      },
      {
        "elementType": "labels.icon",
        "stylers": [
          { "visibility": "on" }
        ]
      },
      {
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#a1a1aa" }
        ]
      },
      {
        "elementType": "labels.text.stroke",
        "stylers": [
          { "color": "#18181b" }
        ]
      },
      {
        "featureType": "administrative",
        "elementType": "geometry",
        "stylers": [
          { "color": "#3f3f46" }
        ]
      },
      {
        "featureType": "administrative.country",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#e4e4e7" }
        ]
      },
      {
        "featureType": "administrative.locality",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#f4f4f5" }
        ]
      },
      {
        "featureType": "poi",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#38bdf8" }
        ]
      },
      {
        "featureType": "poi.medical",
        "elementType": "geometry",
        "stylers": [
          { "color": "#451a1a" }
        ]
      },
      {
        "featureType": "poi.medical",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#f87171" }
        ]
      },
      {
        "featureType": "poi.school",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#4ade80" }
        ]
      },
      {
        "featureType": "poi.business",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#fbbf24" }
        ]
      },
      {
        "featureType": "poi.government",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#60a5fa" }
        ]
      },
      {
        "featureType": "poi.park",
        "elementType": "geometry",
        "stylers": [
          { "color": "#14281d" }
        ]
      },
      {
        "featureType": "poi.park",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#22c55e" }
        ]
      },
      {
        "featureType": "road",
        "elementType": "geometry",
        "stylers": [
          { "color": "#27272a" }
        ]
      },
      {
        "featureType": "road",
        "elementType": "geometry.stroke",
        "stylers": [
          { "color": "#18181b" }
        ]
      },
      {
        "featureType": "road",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#d4d4d8" }
        ]
      },
      {
        "featureType": "road.highway",
        "elementType": "geometry",
        "stylers": [
          { "color": "#3b82f6" }
        ]
      },
      {
        "featureType": "road.highway",
        "elementType": "geometry.stroke",
        "stylers": [
          { "color": "#1e3a8a" }
        ]
      },
      {
        "featureType": "road.highway",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#ffffff" }
        ]
      },
      {
        "featureType": "road.arterial",
        "elementType": "geometry",
        "stylers": [
          { "color": "#3f3f46" }
        ]
      },
      {
        "featureType": "transit",
        "elementType": "geometry",
        "stylers": [
          { "color": "#27272a" }
        ]
      },
      {
        "featureType": "transit.station",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#38bdf8" }
        ]
      },
      {
        "featureType": "water",
        "elementType": "geometry",
        "stylers": [
          { "color": "#0c2340" }
        ]
      },
      {
        "featureType": "water",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#38bdf8" }
        ]
      }
    ]"""

    /**
     * Crisp High-Clarity Street Navigation Style
     * Enhanced visibility of place names, street typography, and distinct POI colors.
     */
    const val CLEAN_STREET_JSON = """[
      {
        "featureType": "poi",
        "elementType": "labels.icon",
        "stylers": [
          { "visibility": "on" }
        ]
      },
      {
        "featureType": "poi.medical",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#dc2626" }
        ]
      },
      {
        "featureType": "poi.school",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#15803d" }
        ]
      },
      {
        "featureType": "poi.government",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#1d4ed8" }
        ]
      },
      {
        "featureType": "poi.business",
        "elementType": "labels.text.fill",
        "stylers": [
          { "color": "#b45309" }
        ]
      },
      {
        "featureType": "road.highway",
        "elementType": "geometry.fill",
        "stylers": [
          { "color": "#2563eb" }
        ]
      },
      {
        "featureType": "road.arterial",
        "elementType": "geometry.fill",
        "stylers": [
          { "color": "#ffffff" }
        ]
      },
      {
        "featureType": "water",
        "elementType": "geometry.fill",
        "stylers": [
          { "color": "#bae6fd" }
        ]
      }
    ]"""
}
