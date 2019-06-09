#include <GL/glut.h>
#include <cmath>
#include <cstdlib>
#include <cstdio>


#include <thread>
#include <chrono>
#include <iostream>
#include <fstream>
#include <vector>
#include <string>

#include "src/Scene.h"

const float PI = 3.14159f;
float camera_theta = 2.042033;
float camera_phi = 8.639376;
float camera_r = 15;
static GLint window;
static unsigned int SCREENWIDTH = 640;
static unsigned int SCREENHEIGHT = 480;
Scene scene;
double aspect_ratio = 0;


void reshape(int w, int h)
{
    aspect_ratio = (double)w / (double)h;
    glViewport(0, 0, w, h);
}

void init()
{
    glClearColor(1,1,1,0);
    glPolygonMode(GL_FRONT_AND_BACK,GL_FILL);
}
void draw () {
    scene.draw();
}

void myaxes(double size)
{
    glBegin(GL_LINES);
    glColor3f(0,0,0);
    glVertex3f(0,0,0); 
    glColor3f(1,0,0);
    glVertex3f(size,0,0); //x-axis

    glColor3f(0,0,0);
    glVertex3f(0,0,0); 
    glColor3f(0,1,0);
    glVertex3f(0,size,0); //y-axis

    glColor3f(0,0,0);
    glVertex3f(0,0,0);
    glColor3f(0,0,1);
    glVertex3f(0,0,size); //z-axis
    glEnd();
}

void display(void)
{
    glEnable(GL_DEPTH_TEST);
    // Clear Color and Depth Buffers
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glMatrixMode(GL_PROJECTION);
    //reset transformations
    glLoadIdentity();
    gluPerspective(45, aspect_ratio, 1, 100);

    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();

    // spherical coordinate camera transform, +Z is "up"
    glTranslatef(0 ,0 , -camera_r);
    glRotatef( (camera_theta - PI) * (180.0f/PI), 1,0,0 );
    glRotatef( -camera_phi * (180.0f/PI), 0,0,1 );

    // draw the model
    draw ();
    myaxes(5);
    glutSwapBuffers();
}

void mykeyboardcontrol(unsigned char key, int x, int y)
{
    switch(key){
        case 'r': camera_r+=0.1;break; //increase radius
        case 'p': camera_r-=0.1;break; //decrease radius

        case 'i': camera_theta+=PI/20;break;//increase theta angle
        case 'k': camera_theta-=PI/20;break;//increase theta angle
        case 'j': camera_phi-=PI/20;break;//increase phi angle
        case 'l': camera_phi+=PI/20;break;//increase phi angle
        }
    printf("r=%f  theta=%f  phi=%f\n",camera_r,camera_theta,camera_phi);
    if(key==27) exit(0);

    // clamp theta 
    if( camera_theta < 0 ) camera_theta = 0;
    if( camera_theta > PI ) camera_theta = PI;

    // wrap phi
    if( camera_phi > 2*PI ) camera_phi -= 2*PI;
    if( camera_phi < 2*PI ) camera_phi += 2*PI;

    printf("r=%f  theta=%f  phi=%f\n",camera_r,camera_theta,camera_phi);

    glutPostRedisplay();
}

int main(int argc, char **argv)
{   // init GLUT and create window
    glutInit(&argc, argv);
    glutInitDisplayMode(GLUT_RGBA | GLUT_DEPTH | GLUT_DOUBLE);
    glutInitWindowSize (SCREENWIDTH, SCREENHEIGHT);
    window=glutCreateWindow ("Multiviews generator");
    init();

    // register callbacks
    glutDisplayFunc(display);
    glutReshapeFunc(reshape);
    glutKeyboardFunc(mykeyboardcontrol);

    //display the model
    scene.addMesh ("models/chair.off");
    
    glutMainLoop();

    return EXIT_SUCCESS;
}