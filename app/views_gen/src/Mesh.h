#ifndef MESH_H
#define MESH_H


#include <vector>
#include <string>
#include "Vec3.h"

#include <GL/glut.h>


// -------------------------------------------
// Basic Mesh class
// -------------------------------------------

struct MeshVertex {
    inline MeshVertex () {}
    inline MeshVertex (const Vec3 & _p, const Vec3 & _n) : p (_p), n (_n) {}
    inline MeshVertex (const MeshVertex & v) : p (v.p), n (v.n) {}
    inline virtual ~MeshVertex () {}
    inline MeshVertex & operator = (const MeshVertex & v) {
        p = v.p;
        n = v.n;
        return (*this);
    }
    // membres :
    Vec3 p; // une position
    Vec3 n; // une normale
};

struct MeshTriangle {
    inline MeshTriangle () {
        corners[0] = corners[1] = corners[2] = 0;
    }
    inline MeshTriangle (const MeshTriangle & t) {
        corners[0] = t[0];   corners[1] = t[1];   corners[2] = t[2];
    }
    inline MeshTriangle (unsigned int v0, unsigned int v1, unsigned int v2) {
        corners[0] = v0;   corners[1] = v1;   corners[2] = v2;
    }
    inline virtual ~MeshTriangle () {}
    inline MeshTriangle & operator = (const MeshTriangle & t) {
        corners[0] = t[0];   corners[1] = t[1];   corners[2] = t[2];
        return (*this);
    }

    unsigned int operator [] (unsigned int c) const { return corners[c]; }
    unsigned int & operator [] (unsigned int c) { return corners[c]; }

private:
    // membres :
    unsigned int corners[3];
};


class Mesh {
public:
    std::vector<MeshVertex>   vertices;
    std::vector<MeshTriangle> triangles;
    std::vector<float> positionArray;
    std::vector<int> triangleArray;
    std::vector<float> normalArray;
    std::vector<float> colorArray;

    void loadOFF (const std::string & filename);
    void recomputeNormals ();
    void centerAndScaleToUnit ();
    void scaleUnit ();

    void buildTriangleArrays(){
    	for(unsigned int t=0;t < triangles.size();t++){
    		const MeshTriangle & tr=triangles[t];
    		triangleArray.push_back( tr[0] );
    		triangleArray.push_back( tr[1] );
    		triangleArray.push_back( tr[2] );
		}
		for(unsigned int t=0;t < vertices.size();t++){
    		const MeshVertex & v=vertices[t];
    		positionArray.push_back( v.p[0] );
    		positionArray.push_back( v.p[1] );
    		positionArray.push_back( v.p[2] );
		}
    }
    void buildVertexArray(){
		for(unsigned int t=0;t < vertices.size();t++){
    		const MeshVertex & v=vertices[t];
    		normalArray.push_back( v.n[0] );
    		normalArray.push_back( v.n[1] );
    		normalArray.push_back( v.n[2] );
		}
    }
    void buildColorArray(){
    	for(unsigned int t=0;t < vertices.size();t++){
    		colorArray.push_back((float) rand()/((float)RAND_MAX));
    		colorArray.push_back((float) rand()/((float)RAND_MAX));
    		colorArray.push_back((float) rand()/((float)RAND_MAX));
		}
    }
    void draw() const {
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);
        glEnable(GL_COLOR_MATERIAL);
        glVertexPointer(3,GL_FLOAT,3*sizeof(float),(GLvoid*)(&positionArray[0]));
        glNormalPointer(GL_FLOAT,3*sizeof(float),(GLvoid*)(&normalArray[0]));
        glColorPointer(3,GL_FLOAT,3*sizeof(float),(GLvoid*)(&colorArray[0]));
        glDrawElements(GL_TRIANGLES,triangleArray.size(),GL_UNSIGNED_INT,(GLvoid*)(&triangleArray[0]));
        
    }
};
#endif
