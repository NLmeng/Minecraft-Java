#version 330 core

in vec2 vTexCoord;
in vec3 vColor;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform bool uIsWater;

void main() {
    // sample the texture
    vec4 texColor = texture(uTexture, vTexCoord);

    // multiply by vertex color
    vec3 finalColor = texColor.rgb * vColor;

    // partial transparency
    if(uIsWater) {
        FragColor = vec4(finalColor, 0.5);
    } else {
        FragColor = vec4(finalColor, texColor.a);
    }
}
