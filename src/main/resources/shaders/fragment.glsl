#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uTexture;
// applies textures, turn vec3 (rgb) to vec4 (rgba)
void main() {
    FragColor = texture(uTexture, vTexCoord);
}
