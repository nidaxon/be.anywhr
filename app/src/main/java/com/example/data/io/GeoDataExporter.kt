package com.example.data.io

import com.example.data.model.BookmarkEntity
import com.example.data.model.SavedRouteEntity
import java.io.StringWriter

object GeoDataExporter {

    fun exportBookmarksToCsv(bookmarks: List<BookmarkEntity>): String {
        val writer = StringWriter()
        writer.append("Name,Latitude,Longitude,Altitude,Category,Description,ColorHex,CreatedAt\n")
        for (b in bookmarks) {
            val cleanName = escapeCsv(b.name)
            val cleanDesc = escapeCsv(b.description)
            val cleanCat = escapeCsv(b.category)
            writer.append("$cleanName,${b.latitude},${b.longitude},${b.altitude},$cleanCat,$cleanDesc,${b.colorHex},${b.createdAt}\n")
        }
        return writer.toString()
    }

    fun parseCsvBookmarks(csvContent: String): List<BookmarkEntity> {
        val list = mutableListOf<BookmarkEntity>()
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return list

        val startIndex = if (lines[0].contains("Latitude", ignoreCase = true)) 1 else 0

        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            val tokens = parseCsvLine(line)
            if (tokens.size >= 3) {
                try {
                    val name = tokens[0]
                    val lat = tokens[1].toDoubleOrNull()
                    val lon = tokens[2].toDoubleOrNull()
                    if (lat != null && lon != null) {
                        val alt = tokens.getOrNull(3)?.toDoubleOrNull() ?: 15.0
                        val cat = tokens.getOrNull(4)?.ifBlank { "Imported" } ?: "Imported"
                        val desc = tokens.getOrNull(5) ?: ""
                        val color = tokens.getOrNull(6)?.ifBlank { "#06B6D4" } ?: "#06B6D4"

                        list.add(
                            BookmarkEntity(
                                name = name,
                                latitude = lat,
                                longitude = lon,
                                altitude = alt,
                                category = cat,
                                description = desc,
                                colorHex = color
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
        return list
    }

    fun exportBookmarksToKml(bookmarks: List<BookmarkEntity>, routes: List<SavedRouteEntity> = emptyList()): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append("\n")
        sb.append("  <Document>\n")
        sb.append("    <name>GeoShift Saved Locations</name>\n")
        sb.append("    <description>Exported coordinates and waypoints from GeoShift Mock GPS</description>\n")

        sb.append("    <Folder>\n")
        sb.append("      <name>Bookmarks</name>\n")
        for (b in bookmarks) {
            sb.append("      <Placemark>\n")
            sb.append("        <name>${escapeXml(b.name)}</name>\n")
            sb.append("        <description>${escapeXml("${b.description} [Category: ${b.category}]")}</description>\n")
            sb.append("        <Point>\n")
            // KML coordinates are lon,lat,alt
            sb.append("          <coordinates>${b.longitude},${b.latitude},${b.altitude}</coordinates>\n")
            sb.append("        </Point>\n")
            sb.append("      </Placemark>\n")
        }
        sb.append("    </Folder>\n")

        if (routes.isNotEmpty()) {
            sb.append("    <Folder>\n")
            sb.append("      <name>Routes</name>\n")
            for (r in routes) {
                sb.append("      <Placemark>\n")
                sb.append("        <name>${escapeXml(r.name)}</name>\n")
                sb.append("        <description>${escapeXml("Category: ${r.category}, Speed: ${r.speedKmh} km/h, Mode: ${r.mode}")}</description>\n")
                sb.append("        <LineString>\n")
                sb.append("          <coordinates>\n")
                val coordPairs = parseRoutePointsString(r.waypointsJson)
                for (pt in coordPairs) {
                    sb.append("            ${pt.second},${pt.first},0\n")
                }
                sb.append("          </coordinates>\n")
                sb.append("        </LineString>\n")
                sb.append("      </Placemark>\n")
            }
            sb.append("    </Folder>\n")
        }

        sb.append("  </Document>\n")
        sb.append("</kml>")
        return sb.toString()
    }

    fun parseKmlBookmarks(kmlContent: String): List<BookmarkEntity> {
        val list = mutableListOf<BookmarkEntity>()
        // Lightweight regex-based KML placemark extractor
        val placemarkRegex = Regex("<Placemark>(.*?)</Placemark>", RegexOption.DOT_MATCHES_ALL)
        val nameRegex = Regex("<name>(.*?)</name>")
        val descRegex = Regex("<description>(.*?)</description>")
        val coordsRegex = Regex("<coordinates>(.*?)</coordinates>")

        for (match in placemarkRegex.findAll(kmlContent)) {
            val content = match.groupValues[1]
            val name = nameRegex.find(content)?.groupValues?.get(1)?.trim() ?: "Imported Point"
            val desc = descRegex.find(content)?.groupValues?.get(1)?.trim() ?: ""
            val coordsStr = coordsRegex.find(content)?.groupValues?.get(1)?.trim() ?: continue

            // Split by comma or space
            val parts = coordsStr.trim().split(Regex("[,\\s]+"))
            if (parts.size >= 2) {
                // KML is lon, lat, (alt)
                val lon = parts[0].toDoubleOrNull()
                val lat = parts[1].toDoubleOrNull()
                val alt = parts.getOrNull(2)?.toDoubleOrNull() ?: 15.0
                if (lat != null && lon != null) {
                    val category = if (desc.contains("Category: ")) {
                        desc.substringAfter("Category: ").substringBefore("]").substringBefore(",").trim()
                    } else "Imported"

                    list.add(
                        BookmarkEntity(
                            name = name,
                            latitude = lat,
                            longitude = lon,
                            altitude = alt,
                            category = category.ifBlank { "Imported" },
                            description = desc
                        )
                    )
                }
            }
        }
        return list
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun parseRoutePointsString(jsonOrCsv: String): List<Pair<Double, Double>> {
        val res = mutableListOf<Pair<Double, Double>>()
        val clean = jsonOrCsv.replace("[", "").replace("]", "").replace("\"", "")
        for (item in clean.split(";", "|")) {
            val coords = item.split(",")
            if (coords.size >= 2) {
                val lat = coords[0].toDoubleOrNull()
                val lon = coords[1].toDoubleOrNull()
                if (lat != null && lon != null) {
                    res.add(Pair(lat, lon))
                }
            }
        }
        return res
    }
}
