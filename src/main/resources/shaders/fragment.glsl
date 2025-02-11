#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform bool uIsWater;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    if(uIsWater) {
        // For water, output with 50% transparency (adjust as needed)
        FragColor = vec4(texColor.rgb, 0.5);
    } else {
        FragColor = vec4(texColor.rgb, 1.0);
    }
}
