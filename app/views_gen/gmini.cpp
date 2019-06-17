#include <GL/glut.h>
#include <GL/glext.h>
#include <GL/gl.h>
#include <GL/glu.h>
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
using namespace std;


std::string modelName;
float camera_theta = 2.042033;
float camera_phi = 0;
float camera_r = 15;
static GLint window;
static const GLenum FORMAT = GL_RGBA;
static unsigned int SCREENWIDTH = 640;
static unsigned int SCREENHEIGHT = 480;
static const GLuint FORMAT_NBYTES = 4;
Scene scene;
double aspect_ratio = 0;
int cp=0;
void reshape(int width, int high)
{
    aspect_ratio = (double)width / (double)high;
    glViewport(0, 0, width, high);
}

void init()
{
    glClearColor(1,1,1,0);
    glPolygonMode(GL_FRONT_AND_BACK,GL_FILL);
}
void axes()
{
    glBegin(GL_LINES);
    //axis X
    glColor3f(0,0,0);
    glVertex3f(0,0,0); 
    glColor3f(1,0,0);
    glVertex3f(4,0,0);
    //axis Y
    glColor3f(0,0,0);
    glVertex3f(0,0,0); 
    glColor3f(0,1,0);
    glVertex3f(0,4,0); 
    //axis Z
    glColor3f(0,0,0);
    glVertex3f(0,0,0);
    glColor3f(0,0,1);
    glVertex3f(0,0,4); 
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
    glRotatef( (camera_theta - M_PI) * (180.0f/M_PI), 1,0,0 );
    glRotatef( -camera_phi * (180.0f/M_PI), 0,0,1 );

    // draw the model
    scene.draw();
    //axes();
    glutSwapBuffers();
}
void saveView() {    
    const int nbPix = SCREENWIDTH * SCREENHEIGHT * 3;
    unsigned char pixels[nbPix];
    char fileName[12];
    sprintf(fileName, "%d", cp);

    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadBuffer(GL_FRONT);
    glReadPixels(0, 0, SCREENWIDTH, SCREENWIDTH, GL_BGR_EXT, GL_UNSIGNED_BYTE, pixels);

    FILE *outputFile = fopen(fileName+".tga", "w");
    short header[] = {0, 2, 0, 0, 0, 0, (short) SCREENWIDTH, (short) SCREENWIDTH, 24};

    fwrite(&header, sizeof(header), 1, outputFile);
    fwrite(pixels, nbPix, 1, outputFile);
    fclose(outputFile);

    printf("View %d captured\n",cp);
    cp++;
}
void processNormalKeys(unsigned char key, int x, int y)
{
    switch(key){
        case '-': 
            camera_r+=0.5;
            break; 
        case '+':       
            camera_r-=0.5;
            break; 
        case 's':
            //pixels =  ( GLubyte *) malloc(FORMAT_NBYTES * SCREENWIDTH * SCREENHEIGHT);
            saveView();
            //free(pixels);
            break;
        default:
            break;
        }
    printf("r=%f  theta=%f  phi=%f\n",camera_r,camera_theta,camera_phi);
    if(key==27) exit(0);

    // clamp theta 
    if( camera_theta < 0 ) camera_theta = 0;
    if( camera_theta > M_PI ) camera_theta = M_PI;

    // wrap phi
    if( camera_phi > 2*M_PI ) camera_phi -= 2*M_PI;
    if( camera_phi < 2*M_PI ) camera_phi += 2*M_PI;

    printf("r=%f  theta=%f  phi=%f\n",camera_r,camera_theta,camera_phi);

    glutPostRedisplay();
}
void processSpecialKeys(int key, int x, int y)
{
    switch(key){
        case GLUT_KEY_UP: 
            camera_theta+=M_PI/30;
            break;
        case GLUT_KEY_DOWN: 
            camera_theta-=M_PI/30;
            break;
        case GLUT_KEY_LEFT: 
            camera_phi-=M_PI*30.0f/180.0f;
            break;
        case GLUT_KEY_RIGHT: 
            camera_phi+= M_PI*30.0f/180.0f;
            break;
        default:
            break;
        }
    printf("r=%f  theta=%f  phi=%f\n",camera_r,camera_theta,camera_phi);
    if(key==27) exit(0);

    // clamp theta 
    if( camera_theta < 0 ) camera_theta = 0;
    if( camera_theta > M_PI ) camera_theta = M_PI;

    // wrap phi
    if( camera_phi > 2*M_PI ) camera_phi -= 2*M_PI;
    if( camera_phi < 2*M_PI ) camera_phi += 2*M_PI;

    printf("r=%f  theta=%f  phi=%f\n",camera_r,camera_theta,camera_phi);

    glutPostRedisplay();
}


int main(int argc, char **argv)
{   
     if (argc < 2){
        printf("The name of the model is required\n");
        return 0;
     }
        
    modelName = argv[1];
    printf("Model reading... : %s \n", argv[1]);
    // init GLUT and create window
    glutInit(&argc, argv);
    glutInitDisplayMode(GLUT_RGBA | GLUT_DEPTH | GLUT_DOUBLE);
    glutInitWindowSize (SCREENWIDTH, SCREENHEIGHT);
    window=glutCreateWindow ("Multiviews generator");
    init();

    // register callbacks
    glutDisplayFunc(display);
    glutReshapeFunc(reshape);
    glutKeyboardFunc(processNormalKeys);
    glutSpecialFunc(processSpecialKeys);
    //display the model
    scene.addMesh ("models/"+modelName);
    
    glutMainLoop();

    return EXIT_SUCCESS;
}