/* YUV -> RGB shader for 4:2:2 I420 format (+ rounded rectangles)
 *                                                                            
 * MIT X11 license, Copyright (c) 2007 by:                               
 *                                                                            
 * Authors:                                                                   
 *      Michael Dominic K. <mdk@mdk.am>
 *                                                                            
 * Permission is hereby granted, free of charge, to any person obtaining a   
 *  copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation  
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,   
 * and/or sell copies of the Software, and to permit persons to whom the      
 * Software is furnished to do so, subject to the following conditions:       
 *                                                                            
 * The above copyright notice and this permission notice shall be included    
 * in all copies or substantial portions of the Software.                     
 *                                                                            
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS    
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF                 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN  
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,   
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR      
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE  
 * USE OR OTHER DEALINGS IN THE SOFTWARE.                                     
 *                                                                            
 */

/*
 * This GLSL version is created by Akihiro Komori
 * Date: Jun. 17 2011
 */
#ifdef GL_ARB_texture_rectangle
#extension GL_ARB_texture_rectangle : require
//#extension GL_ARB_texture_rectangle : enable
#endif
    
uniform sampler2DRect texture1;
uniform sampler2DRect texture2;
uniform sampler2DRect texture3;

void main ()
{
    vec3 pre;

    pre.r = texture2DRect (texture1, gl_TexCoord[0].st).r - (16.0 / 256.0);
    pre.g = texture2DRect (texture2, gl_TexCoord[1].st).r - (128.0 / 256.0);
    pre.b = texture2DRect (texture3, gl_TexCoord[2].st).r - (128.0 / 256.0);

    const vec3 red = vec3 (0.00456621, 0.0, 0.00625893) * 255.0;
    const vec3 green = vec3 (0.00456621, -0.00153632, -0.00318811) * 255.0;
    const vec3 blue = vec3 (0.00456621, 0.00791071, 0.0) * 255.0;
    
    gl_FragColor.r = dot (red, pre);
    gl_FragColor.g = dot (green, pre);
    gl_FragColor.b = dot (blue, pre);

#if 1
    /* The stuff below is a poorman's-ugly-hacky rounded rectangles and
     * smooth borders implementation. Just replace it with:
       gl_FragColor.a = gl_Color.a;
       if you're interested only in the YUV->RGB methodology */

    float r;

    if (gl_TexCoord[0].s < 21.0 && gl_TexCoord[0].t < 21.0) 
        r = (gl_TexCoord[0].s - 21.0) * (gl_TexCoord[0].s - 21.0) + (gl_TexCoord[0].t - 21.0) * (gl_TexCoord[0].t - 21.0);
    else if (gl_TexCoord[0].s > 379.0 && gl_TexCoord[0].t < 21.0)
        r = (gl_TexCoord[0].s - 379.0) * (gl_TexCoord[0].s - 379.0) + (gl_TexCoord[0].t - 21.0) * (gl_TexCoord[0].t - 21.0);
    else if (gl_TexCoord[0].s > 379.0 && gl_TexCoord[0].t > 219.0)
        r = (gl_TexCoord[0].s - 379.0) * (gl_TexCoord[0].s - 379.0) + (gl_TexCoord[0].t - 219.0) * (gl_TexCoord[0].t - 219.0);
    else if (gl_TexCoord[0].s < 21.0 && gl_TexCoord[0].t > 219.0)
        r = (gl_TexCoord[0].s - 21.0) * (gl_TexCoord[0].s - 21.0) + (gl_TexCoord[0].t - 219.0) * (gl_TexCoord[0].t - 219.0);
    else if (gl_TexCoord[0].t < 21.0)
        r = (gl_TexCoord[0].t - 21.0) * (gl_TexCoord[0].t - 21.0);
    else if (gl_TexCoord[0].t > 219.0)
        r = (gl_TexCoord[0].t - 219.0) * (gl_TexCoord[0].t - 219.0);
    else if (gl_TexCoord[0].s < 21.0)
        r = (gl_TexCoord[0].s - 21.0) * (gl_TexCoord[0].s - 21.0);
    else if (gl_TexCoord[0].s > 379.0)
        r = (gl_TexCoord[0].s - 379.0) * (gl_TexCoord[0].s - 379.0);
    else
        r = 400.0;

    float v = clamp (r, 400.0, 440.0);
    v = (v - 400.0) / 40.0;

    gl_FragColor.a = gl_Color.a * (1.0 - v);
#else
    gl_FragColor.a = gl_Color.a;
#endif
}

/* vim: set et fenc=utf-8 ff=unix syntax=glsl */

