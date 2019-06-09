#ifndef SCENE_H
#define SCENE_H

#include <vector>
#include <string>
#include "Mesh.h"

#include <GL/glut.h>

class Scene {
   

public:
     std::vector< Mesh > meshes;
    Scene() {}
    void addMesh(std::string const & modelFilename) {
        meshes.resize( meshes.size() + 1 );
        Mesh & m = meshes[ meshes.size() - 1 ];
        m.loadOFF (modelFilename);
        m.centerAndScaleToUnit ();
        m.recomputeNormals ();
        m.buildTriangleArrays();
        m.buildVertexArray();         
        //meshAjoute.buildColorArray();
    }
    void draw() const {
        // iterer sur l'ensemble des objets, et faire leur rendu.
        for( unsigned int mIt = 0 ; mIt < meshes.size() ; ++mIt ) {
            Mesh const & mesh = meshes[mIt];
            mesh.draw();
        }
    }
};

#endif
