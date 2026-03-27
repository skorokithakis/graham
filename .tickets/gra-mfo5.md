---
id: gra-mfo5
status: closed
deps: []
links: []
created: 2026-03-27T17:39:14Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Shrink launcher icon foreground to fit adaptive icon safe zone

In ic_launcher_foreground.xml, the paths are scaled 3x from a 24x24 source and offset by 18dp. This makes the content span 19-89 on x, which is too large for the 66dp safe zone (21-87) and gets clipped during launcher parallax. Re-scale all paths to 2.25x from the 24x24 source and offset by (108-54)/2 = 27dp. That means: multiply all original 24x24 coordinates by 2.25, then add 27 to both x and y. Update the comment to match. The original 24x24 path coordinates can be derived by reversing the current transform: subtract 18, divide by 3.

