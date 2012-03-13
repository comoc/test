/* Example showing hardware RGB -> YUV conversion for 4:2:2 I420 format
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
 This GLSL version is created by Akihiro Komori
*/

#ifndef GL_GLEXT_PROTOTYPES
#define GL_GLEXT_PROTOTYPES
#endif

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <gst/gst.h>
#ifdef __APPLE__
#include <OpenGL/gl.h>
#include <OpenGL/glext.h>
#include <OpenGL/glu.h>
#include <GLUT/glut.h>
#else
#include <GL/gl.h>
#include <GL/glext.h>
#include <GL/glu.h>
#include <GL/glut.h>
#endif
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <sys/stat.h> 
#include <fcntl.h>
#include <pthread.h>

#define WINDOW_WIDTH 640
#define WINDOW_HEIGHT 480
#define VIDEO_WIDTH 400
#define VIDEO_HEIGHT 240

#define VIDEO_FILE "video.avi"
#define FRAGMENT_SHADER_FILE "yuv.frag"

GstElement *pipeline;
GstElement *source;
GstElement *decodebin;
GstElement *videosink;
GstElement *audiosink;
GstElement *audioconvert;
GstElement *audioqueue;
GstElement *videoqueue;
GstBuffer *buffer = NULL;

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

/* Playback start for poorman's syncing */
int64_t start_time = 0;

/* Texture ID's */
GLuint y_plane;
GLuint u_plane;
GLuint v_plane;

/* Shader ID's */
GLuint yuv_shader;

/*{{{*/
static int64_t get_time (void)
{
    struct timeval tv;
    
    gettimeofday(&tv, NULL);

    return (int64_t)tv.tv_sec * 1000000 + (int64_t)tv.tv_usec;
}
/*}}}*/

/*{{{*/
static void draw_background_gradient (void)
{
    glMatrixMode (GL_PROJECTION);

    glPushMatrix ();
    glLoadIdentity ();
    glOrtho (-100, 100, 100, -100, -1000.0, 1000.0);

    glBegin (GL_QUADS);

    glColor4f (0.0, 0.0, 0.0, 1.0);
    glVertex2f (-100.0, -100.0);
    glVertex2f (100.0, -100.0);

    glColor4f (0.0, 0.0, 0.2, 1.0);
    glVertex2f (100.0, 80.0);
    glVertex2f (-100.0, 80.0);

    glVertex2f (100.0, 80.0);
    glVertex2f (-100.0, 80.0);

    glVertex2f (-100.0, 100.0);
    glVertex2f (100.0, 100.0);

    glEnd ();
    glPopMatrix ();

    glMatrixMode (GL_MODELVIEW);
}
/*}}}*/

/*{{{*/
static GLboolean is_extension_supported(const char* name)
{
    const char* exts;
    if (name == NULL)
        return GL_FALSE;
        
    exts = (const char *)glGetString(GL_EXTENSIONS);
    if (exts == NULL)
        return GL_FALSE;
 
    if (strstr(exts, name) != NULL)
        return GL_TRUE;

    return GL_FALSE;
}
/*}}}*/

/*{{{*/
static void draw_video_plane (float center_x, 
                  float center_y, 
                  float start_alpha, 
                  float stop_alpha, 
                  int reversed)
{

    GLfloat topy;
    GLfloat bottomy;
    if (! reversed) {
        topy = center_y - 1.0f;
        bottomy = center_y + 1.0f;
    } else {
        topy = center_y + 1.0f;
        bottomy = center_y - 1.0f;
    }

    glBegin (GL_QUADS);

    glColor4f (1.0f, 1.0f, 1.0f, start_alpha);

    glMultiTexCoord2f (GL_TEXTURE0_ARB, 0, VIDEO_HEIGHT); 
    glMultiTexCoord2f (GL_TEXTURE1_ARB, 0, VIDEO_HEIGHT / 2); 
    glMultiTexCoord2f (GL_TEXTURE2_ARB, 0, VIDEO_HEIGHT / 2); 
    glVertex2f (center_x - 1.6f, topy);

    glMultiTexCoord2f (GL_TEXTURE0_ARB, VIDEO_WIDTH, VIDEO_HEIGHT); 
    glMultiTexCoord2f (GL_TEXTURE1_ARB, VIDEO_WIDTH / 2, VIDEO_HEIGHT / 2); 
    glMultiTexCoord2f (GL_TEXTURE2_ARB, VIDEO_WIDTH / 2, VIDEO_HEIGHT / 2); 
    glVertex2f (center_x + 1.6f, topy);

    glColor4f (1.0f, 1.0f, 1.0f, stop_alpha);

    glMultiTexCoord2f (GL_TEXTURE0_ARB, VIDEO_WIDTH, 0); 
    glMultiTexCoord2f (GL_TEXTURE1_ARB, VIDEO_WIDTH / 2, 0); 
    glMultiTexCoord2f (GL_TEXTURE2_ARB, VIDEO_WIDTH / 2, 0); 
    glVertex2f (center_x + 1.6f, bottomy);

    glMultiTexCoord2f (GL_TEXTURE0_ARB, 0, 0); 
    glMultiTexCoord2f (GL_TEXTURE1_ARB, 0, 0); 
    glMultiTexCoord2f (GL_TEXTURE2_ARB, 0, 0); 
    glVertex2f (center_x - 1.6f, bottomy);

    glEnd ();

}
/*}}}*/

/*{{{*/
static void resize(int width, int height)
{
    glViewport (0, 0, width, height);

    glMatrixMode (GL_PROJECTION);
    glLoadIdentity ();
    gluPerspective (80, (GLfloat)width / (GLfloat)height, 1.0, 5000.0);
    glMatrixMode (GL_MODELVIEW);
    glLoadIdentity ();
    glTranslatef (0.0f, 0.0f, -3.0f);
}
/*}}}*/

/*{{{*/
static char* create_buffer_from_file(const char* filename)
{
    FILE *fp;
    long file_size;
    char *buf;
    struct stat stbuf;
    int fd;

    if (filename == NULL)
        return NULL;
    
    fd = open(filename, O_RDONLY);
    if (fd == -1) {
      return NULL;
    }
    
    fp = fdopen(fd, "r");
    if (fp == NULL) {
      close(fd);
      return NULL;
    }
    
    if (fstat(fd, &stbuf) == -1) {
      fclose(fp);
      close(fd);
      return NULL;
    }
    file_size = stbuf.st_size;
    
    buf = (char*)malloc(file_size);
    if (buf == NULL) {
      fclose(fp);
      close(fd);
      return NULL;
    }

    fseek(fp,  0L, SEEK_SET);
    fread(buf, file_size, 1, fp);

    fclose(fp);
    close(fd);

    return buf;
}
/*}}}*/

/*{{{*/
static void printShaderInfoLog(GLuint shader)
{
  GLsizei bufSize;
  
  glGetShaderiv(shader, GL_INFO_LOG_LENGTH , &bufSize);
  
  if (bufSize > 1) {
    GLchar *infoLog;
    
    infoLog = (GLchar *)malloc(bufSize);
    if (infoLog != NULL) {
      GLsizei length;
      
      glGetShaderInfoLog(shader, bufSize, &length, infoLog);
      fprintf(stderr, "InfoLog:\n%s\n\n", infoLog);
      free(infoLog);
    }
    else
      fprintf(stderr, "Could not allocate InfoLog buffer.\n");
  }
}
/*}}}*/

/*{{{*/
static void printProgramInfoLog(GLuint program)
{
  GLsizei bufSize;
  
  glGetProgramiv(program, GL_INFO_LOG_LENGTH , &bufSize);
  
  if (bufSize > 1) {
    GLchar *infoLog;
    
    infoLog = (GLchar *)malloc(bufSize);
    if (infoLog != NULL) {
      GLsizei length;
      
      glGetProgramInfoLog(program, bufSize, &length, infoLog);
      fprintf(stderr, "InfoLog:\n%s\n\n", infoLog);
      free(infoLog);
    }
    else
      fprintf(stderr, "Could not allocate InfoLog buffer.\n");
  }
}
/*}}}*/

/*{{{*/
static GLuint load_shader(const char* filename)
{
    char* buf;
    GLuint fragShader;
    GLint compiled, linked;
    GLuint program;

    buf = create_buffer_from_file(filename);

    if (buf == NULL) {
        fprintf(stderr, "Failed to load the shader program from file!\n");
        exit(0);
    }

    fragShader = glCreateShader(GL_FRAGMENT_SHADER);

    glShaderSource(fragShader, 1, (const GLchar**)&buf, NULL); 
    free(buf);

    glCompileShader(fragShader);
    glGetShaderiv(fragShader, GL_COMPILE_STATUS, &compiled);
    printShaderInfoLog(fragShader);
    if (compiled == GL_FALSE) {
        fprintf(stderr, "Compile error in fragment shader.\n");
        exit(1);
    }

    program = glCreateProgram();
    glAttachShader(program, fragShader);
    glDeleteShader(fragShader);
    glLinkProgram(program);
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    printProgramInfoLog(program);
    if (linked == GL_FALSE) {
        fprintf(stderr, "Link error.\n");
        exit(1);
    }
    
    return program;
}
/*}}}*/

/*{{{*/
static gboolean
on_pad_buffer (GstPad *pad, 
               GstBuffer *buf)
{
    int64_t timestamp;
    
    pthread_mutex_lock(&mutex);
    
    timestamp = GST_BUFFER_TIMESTAMP (buf);
    if (buffer) 
        gst_buffer_unref (buffer);

    buffer = buf;
    gst_buffer_ref (buf);

    pthread_mutex_unlock(&mutex);

    if (start_time == 0)
        start_time = get_time ();
    else {
        int64_t time_left = (timestamp / 1000) - (get_time () - start_time);
        time_left -= 1000000 / 25;
        if (time_left > 2000)
            usleep (time_left);

    }

    return TRUE;
}
/*}}}*/

/*{{{*/
static void on_new_pad (GstElement *element,
            GstPad *pad,
            gpointer data)
{
    GstPad *sinkpad;
    GstCaps *caps;
    GstStructure *str;

    caps = gst_pad_get_caps (pad);
    str = gst_caps_get_structure (caps, 0);

    if (g_strrstr (gst_structure_get_name (str), "video")) {
        sinkpad = gst_element_get_pad (videoqueue, "sink");
        gst_pad_add_buffer_probe (pad, G_CALLBACK (on_pad_buffer), NULL);
    } else
        sinkpad = gst_element_get_pad (audioqueue, "sink");

    gst_caps_unref (caps);

    gst_pad_link (pad, sinkpad);
    gst_object_unref (sinkpad);
}
/*}}}*/

/*{{{*/
static void
update_texture_data (GLint tex, 
                     gpointer data,
                     int w, 
                     int h)
{
    glEnable (GL_TEXTURE_RECTANGLE_ARB);
    glBindTexture (GL_TEXTURE_RECTANGLE_ARB, tex);

    glTexParameteri (GL_TEXTURE_RECTANGLE_ARB, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri (GL_TEXTURE_RECTANGLE_ARB, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    glTexImage2D  (GL_TEXTURE_RECTANGLE_ARB, 0, 1, w , h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data);
}
/*}}}*/

/*{{{*/
static void update_textures_from_buffer (void)
{
    glActiveTexture (GL_TEXTURE0_ARB);
    update_texture_data (y_plane, GST_BUFFER_DATA (buffer), VIDEO_WIDTH, VIDEO_HEIGHT);

    glActiveTexture (GL_TEXTURE1_ARB);
    update_texture_data (u_plane, GST_BUFFER_DATA (buffer) + VIDEO_WIDTH * VIDEO_HEIGHT, VIDEO_WIDTH / 2, VIDEO_HEIGHT / 2);

    glActiveTexture (GL_TEXTURE2_ARB);
    update_texture_data (v_plane, GST_BUFFER_DATA (buffer) + VIDEO_WIDTH * VIDEO_HEIGHT + (VIDEO_WIDTH / 2) * (VIDEO_HEIGHT / 2), VIDEO_WIDTH / 2, VIDEO_HEIGHT / 2);    
}
/*}}}*/

/*{{{*/
static void init_gl_resources (void)
{
    glGenTextures (1, &y_plane);
    glGenTextures (1, &u_plane);
    glGenTextures (1, &v_plane);

    glEnable (GL_BLEND);
    glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);  

    yuv_shader = load_shader (FRAGMENT_SHADER_FILE);

    if (yuv_shader <= 0) {
        fprintf(stderr, "Failed to load the shader program!");
        exit(0);
    }
}
/*}}}*/

/*{{{*/
static void 
display(void)
{
    /* Clear */
    glClear (GL_COLOR_BUFFER_BIT);

    draw_background_gradient ();

    glUseProgram(yuv_shader);

    glPushMatrix ();
#if 0
    /* Rotation */
    if (start_time != 0) {
        int64_t time_passed = get_time () - start_time;
        glRotatef (sinf (time_passed / 1200000.0) * 45.0, 0.0, 1.0, 0.0);
    }
#endif
    /* Update textures */
    pthread_mutex_lock(&mutex);
    if (buffer != NULL) {
        update_textures_from_buffer ();
        gst_buffer_unref (buffer);
        buffer = NULL;
    }
    pthread_mutex_unlock(&mutex);
    
    glUniform1i(glGetUniformLocation(yuv_shader, "texture1"), 0);
    glUniform1i(glGetUniformLocation(yuv_shader, "texture2"), 1);
    glUniform1i(glGetUniformLocation(yuv_shader, "texture3"), 2);

    /* Reflection */
    draw_video_plane (0.0f, -2.0f, 0.3f, 0.0f, TRUE);

    /* Main video */
    draw_video_plane (0.0, 0.0, 1.0, 1.0, FALSE);
    
    glUseProgram(0);

    /* Reset textures */
    glActiveTexture (GL_TEXTURE0_ARB); glDisable (GL_TEXTURE_RECTANGLE_ARB);
    glActiveTexture (GL_TEXTURE1_ARB); glDisable (GL_TEXTURE_RECTANGLE_ARB);
    glActiveTexture (GL_TEXTURE2_ARB); glDisable (GL_TEXTURE_RECTANGLE_ARB);

    glPopMatrix ();
    

    {
        GLenum err = glGetError();
        if (err != GL_NO_ERROR) {
            fprintf(stderr,
			        "GL error: %s \n",
			        (const char*)gluErrorString(err));
                
        }

    }

    glutSwapBuffers();
}
/*}}}*/

/*{{{*/
static void idle(void)
{
    glutPostRedisplay();
}
/*}}}*/

/*{{{*/
static void keyboard(unsigned char key , int x , int y)
{
    if (key == 0x1b)
        exit(0);

	printf("Key = %c\n" , key);
}
/*}}}*/

/*{{{*/
static gboolean
init_gst()
{
    /* Create the elements */
    pipeline     = gst_pipeline_new (NULL);
    source       = gst_element_factory_make ("filesrc", "filesrc");
    decodebin    = gst_element_factory_make ("decodebin", "decodebin");
    videosink    = gst_element_factory_make ("fakesink", "videosink");
#ifdef __APPLE__
    audiosink    = gst_element_factory_make ("osxaudiosink", "audiosink");
#else
    audiosink    = gst_element_factory_make ("alsasink", "audiosink");
#endif
    audioconvert = gst_element_factory_make ("audioconvert", "audioconvert");
    audioqueue   = gst_element_factory_make ("queue", "audioqueue");
    videoqueue   = gst_element_factory_make ("queue", "videoqueue");

    if (pipeline == NULL || source == NULL || decodebin == NULL ||
        videosink == NULL || audiosink == NULL || audioconvert == NULL || audioqueue == NULL || 
        videoqueue == NULL) {
        fprintf (stderr, "One of the GStreamer decoding elements is missing\n"
              "\tpipeline    :%p\n" 
              "\tsource      :%p\n"
              "\tdecodebin   :%p\n"
              "\tvideosink   :%p\n"
              "\taudiosink   :%p\n"
              "\taudioconvert:%p\n"
              "\taudioqueue  :%p\n"
              "\tvideoqueue  :%p\n",
              pipeline    , 
              source      ,
              decodebin   ,
              videosink   ,
              audiosink   ,
              audioconvert,
              audioqueue  ,
              videoqueue  
                );
        return FALSE;
    }
    /* Setup the pipeline */
    g_object_set (G_OBJECT (source), "location", VIDEO_FILE, NULL);
#if 0 //def __APPLE__
    /* The audiosink under Mac OSX 10.6.7 didn't work. So I neglect audio stuffs. (Written by Akihiro Komori)*/
    gst_bin_add_many (GST_BIN (pipeline), source, decodebin, videosink, 
                      videoqueue, NULL);
    g_signal_connect (decodebin, "pad-added", G_CALLBACK (on_new_pad), NULL);

    /* Link the elements */
    gst_element_link (source, decodebin);
    gst_element_link (videoqueue, videosink);
#else
    gst_bin_add_many (GST_BIN (pipeline), source, decodebin, videosink, 
                      audiosink, audioconvert, audioqueue, videoqueue, NULL);
    g_signal_connect (decodebin, "pad-added", G_CALLBACK (on_new_pad), NULL);

    /* Link the elements */
    gst_element_link (source, decodebin);
    gst_element_link (audioqueue, audioconvert);
    gst_element_link (audioconvert, audiosink);
    gst_element_link (videoqueue, videosink);
#endif

    /* Now run the pipeline... */
    gst_element_set_state (pipeline, GST_STATE_PLAYING);
    return TRUE;
}
/*}}}*/

/* {{{ */
int
main (int argc,
      char *argv[])
{
    /* Initialize */
    gst_init (&argc, &argv);
    glutInit(&argc, argv);
    glutInitWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA | GLUT_DEPTH);
    glutCreateWindow(argv[0]);

    if (is_extension_supported("GL_ARB_multitexture") == GL_FALSE)
        return -1;
    if (is_extension_supported("GL_ARB_texture_rectangle") == GL_FALSE)
        return -1;
    if (is_extension_supported("GL_ARB_vertex_shader") == GL_FALSE)
        return -1;
    if (is_extension_supported("GL_ARB_fragment_shader") == GL_FALSE)
        return -1;
    if (is_extension_supported("GL_ARB_texture_non_power_of_two") == GL_FALSE)
        return -1;

    glutReshapeFunc(resize);
    glutDisplayFunc(display);
    glutIdleFunc(idle);
    glutKeyboardFunc(keyboard);
    init_gl_resources ();
    
    if (init_gst() == FALSE) {
        fprintf(stderr, "Failed to initialize GStreamer compornent\n");
        exit(0);
    }

    /* Main loop */
    glutMainLoop();
    return 0;
}
/* }}} */

/* vim: set et fenc=utf-8 ff=unix foldmethod=marker: */

