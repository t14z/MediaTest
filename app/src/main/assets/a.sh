precision mediump float;

uniform sampler2D u_Texture;  // The input texture.

varying vec2 v_TexCoordinate;  // Interpolated texture coordinate per fragment.

vec4 motionBlur(sampler2D color, vec2 uv, float intensity)
{
    vec2 speed = vec2(0.05, 0.0);
    vec2 offset = intensity * speed;
    vec3 c = vec3(0.);
    float inc = 0.1;
    float weight = 0.;
    for (float i = 0.; i <= 1.; i += inc)
    {
        c += texture2D(color, uv + i * offset).rgb;
        weight += 1.;
    }
    c /= weight;
    return vec4(c, 1.);
}

void main()  // The entry point for our fragment shader.
{
    gl_FragColor = motionBlur(u_Texture, v_TexCoordinate.xy, 2.8);
    // gl_FragColor = texture2D(u_Texture, v_TexCoordinate);  // Pass the color directly through the pipeline.
}