#version 330 core

layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec2 textureCoords;
layout(location = 2) in vec3 inColor;

out vec2 vTexCoord;
out vec3 vColor;

uniform mat4 uMVP;

void main() {
    vTexCoord = textureCoords;
    vColor    = inColor;   // pass color downstream
    gl_Position = uMVP * vec4(vertexPosition, 1.0);
}