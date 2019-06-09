#include "Mesh.h"
#include <iostream>
#include <fstream>

void Mesh::loadOFF (const std::string & filename) {
    std::ifstream in (filename.c_str ());
    if (!in)
        exit (EXIT_FAILURE);
    std::string offString;
    unsigned int sizeV, sizeT, tmp;
    in >> offString >> sizeV >> sizeT >> tmp;
    vertices.resize (sizeV);
    triangles.resize (sizeT);
    for (unsigned int i = 0; i < sizeV; i++)
        in >> vertices[i].p;
    int s;
    for (unsigned int t = 0; t < sizeT; t++) {
        in >> s;
        for (unsigned int j = 0; j < 3; j++)
            in >> triangles[t][j];
    }
    in.close ();
}

void Mesh::recomputeNormals () {
    for (unsigned int i = 0; i < vertices.size (); i++)
        vertices[i].n = Vec3 (0.0, 0.0, 0.0);
    for (unsigned int t = 0; t < triangles.size (); t++) {
        Vec3 e01 = vertices[  triangles[t][1]  ].p -  vertices[  triangles[t][0]  ].p;
        Vec3 e02 = vertices[  triangles[t][2]  ].p -  vertices[  triangles[t][0]  ].p;
        Vec3 n = Vec3::cross (e01, e02);
        n.normalize ();
        for (unsigned int j = 0; j < 3; j++)
            vertices[  triangles[t][j]  ].n += n;
    }
    for (unsigned int i = 0; i < vertices.size (); i++)
        vertices[i].n.normalize ();
}

void Mesh::centerAndScaleToUnit () {
    Vec3 c(0,0,0);
    for  (unsigned int i = 0; i < vertices.size (); i++)
        c += vertices[i].p;
    c /= vertices.size ();
    float maxD = (vertices[0].p - c).length();
    for (unsigned int i = 0; i < vertices.size (); i++){
        float m = (vertices[i].p - c).length();
        if (m > maxD)
            maxD = m;
    }
    for  (unsigned int i = 0; i < vertices.size (); i++)
        vertices[i].p = (vertices[i].p - c) / maxD;
}
