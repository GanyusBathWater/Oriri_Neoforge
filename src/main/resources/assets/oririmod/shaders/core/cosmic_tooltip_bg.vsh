#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
// Raw GUI-space pixel position forwarded to fsh.
// In GUI rendering, Position.xy == screen pixel coords (top-left origin, GUI scale applied).
// The fsh converts to gl_FragCoord-compatible space for screen-relative UV.
out vec2 vScreenPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor  = Color;
    vScreenPos   = Position.xy;
}
