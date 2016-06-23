precision mediump float;

uniform sampler2D u_Texture;  // The input texture.
varying vec2 v_TexCoordinate;  // Interpolated texture coordinate per fragment.

void main()  // The entry point for our fragment shader.
{
    //this will be our RGBA sum
        vec4 sum = vec4(0.0);

        float resolution = 2.0;
        float radius = 80.0f;
        vec2 dir = vec2(1.0,1.0);
        vec4 v_Color = vec4(1.0,1.0,1.0,1.0);

        //our original texcoord for this fragment
        vec2 tc = v_TexCoordinate;

        //the amount to blur, i.e. how far off center to sample from
        //1.0 -> blur by one pixel
        //2.0 -> blur by two pixels, etc.
        float blur = radius/resolution;

        //the direction of our blur
        //(1.0, 0.0) -> x-axis blur
        //(0.0, 1.0) -> y-axis blur
        float hstep = dir.x;
        float vstep = dir.y;

        //apply blurring, using a 9-tap filter with predefined gaussian weights

        sum += texture2D(u_Texture, vec2(tc.x - 4.0*blur*hstep, tc.y - 4.0*blur*vstep)) * 0.0162162162;
        sum += texture2D(u_Texture, vec2(tc.x - 3.0*blur*hstep, tc.y - 3.0*blur*vstep)) * 0.0540540541;
        sum += texture2D(u_Texture, vec2(tc.x - 2.0*blur*hstep, tc.y - 2.0*blur*vstep)) * 0.1216216216;
        sum += texture2D(u_Texture, vec2(tc.x - 1.0*blur*hstep, tc.y - 1.0*blur*vstep)) * 0.1945945946;

        sum += texture2D(u_Texture, vec2(tc.x, tc.y)) * 0.2270270270;

        sum += texture2D(u_Texture, vec2(tc.x + 1.0*blur*hstep, tc.y + 1.0*blur*vstep)) * 0.1945945946;
        sum += texture2D(u_Texture, vec2(tc.x + 2.0*blur*hstep, tc.y + 2.0*blur*vstep)) * 0.1216216216;
        sum += texture2D(u_Texture, vec2(tc.x + 3.0*blur*hstep, tc.y + 3.0*blur*vstep)) * 0.0540540541;
        sum += texture2D(u_Texture, vec2(tc.x + 4.0*blur*hstep, tc.y + 4.0*blur*vstep)) * 0.0162162162;

        //discard alpha for our simple demo, multiply by vertex color and return
        gl_FragColor = v_Color * vec4(sum.rgb, 1.0);

}