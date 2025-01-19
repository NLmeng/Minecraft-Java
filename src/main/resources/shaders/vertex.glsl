#version 330 core

layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec2 textureCoords;

out vec2 vTexCoord;
uniform mat4 uMVP;
// pass vertex color to fragment, transform from local to screen space
void main() {
    vTexCoord = textureCoords;
    gl_Position = uMVP * vec4(vertexPosition, 1.0);
}
