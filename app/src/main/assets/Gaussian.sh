// 对图像进行高斯模糊

uniform	sampler2D	u_Texture;	// 要进行模糊的源图像
//uniform	vec2		pixelSize;	// 如果纹理大小为800*600, 则为(1.0/800.0, 1.0/600.0)
varying vec2 v_TexCoordinate;

const float e = 2.71828183;

//标准高斯函数，令a或c为参数，可以简化
float Gaussian(float x)	
{
	float Gaussian_a = 2.0;
	float Gaussian_c = 2.0;
	return Gaussian_a * pow(e, -x*x/(Gaussian_c*Gaussian_c) );
}

void main()
{
	vec2 pixelSize = vec2(1.0/300.0 ,1.0/300.0);
	vec2 halfPixelSize = pixelSize.xy / 2.0;
	vec3 cOut = vec3(0.0, 0.0, 0.0);	
	vec2 texCoordSample = v_TexCoordinate[0].st;

	float gaussTotal = 0.0;
	float x,y;
	float temp;
	float RANGE = 3.0;

	for (y=-RANGE; y<=RANGE; y+=1.0)
	{
		for (x=-RANGE; x<=RANGE; x+=1.0)
		{
			// 这一行是在GPU中进行计算，而在实际的程序中如果a,c,RANGE的值已经确定，
			// 可以在CPU中预先计算处所要的权重值，直接代入即可
			temp = Gaussian( sqrt( x*x + y*y ) );
			gaussTotal += temp;
			cOut += temp * vec3( texture2D(u_Texture, texCoordSample + vec2(x,y) * pixelSize) );
		}
	}

	cOut /= gaussTotal;
	
	//调试用：不模糊，输出原图像
	//cOut = vec3(texture2D(u_Texture, v_TexCoordinate[0].st));
	
	gl_FragColor = vec4(cOut, 1.0);
	//gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
}
