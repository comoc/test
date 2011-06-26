/*
 * $Id$
 */

#include <stdio.h>
#include <stdlib.h>

#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif

void display(void)/*{{{*/
{
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glutSwapBuffers();
}/*}}}*/

void idle(void)/*{{{*/
{
    glutPostRedisplay();
}/*}}}*/

void mouse(int button, int state, int x, int y)/*{{{*/
{

}/*}}}*/

void motion(int x, int y)/*{{{*/
{

}/*}}}*/

void keyboard(unsigned char key, int x, int y)/*{{{*/
{
    switch (key) {
    case 'q':
    case 'Q':
    case '\033':
        exit(0);
    default:
        break;
    }
}/*}}}*/

void resize(int w, int h)/*{{{*/
{
    glViewport(0, 0, w, h);
}/*}}}*/

void init(void)/*{{{*/
{
    glClearColor(0, 0, 0, 1);
    glClearDepth(1);
}/*}}}*/

int main(int argc, char *argv[])/*{{{*/
{
    glutInitWindowPosition(100, 100);
    glutInitWindowSize(320, 240);
    glutInit(&argc, argv);
    glutInitDisplayMode(GLUT_RGBA | GLUT_DEPTH | GLUT_DOUBLE);
    glutCreateWindow(argv[0]);
    glutDisplayFunc(display);
    glutIdleFunc(idle);
    glutMouseFunc(mouse);
    glutMotionFunc(motion);
    glutKeyboardFunc(keyboard);
    glutReshapeFunc(resize);
    init();
    glutMainLoop();
    return 0;
}/*}}}*/

/* Modeline for ViM {{{
 * vim:set ts=4:
 * vim600:fdm=marker fdl=0 fdc=3:
 * }}} */


