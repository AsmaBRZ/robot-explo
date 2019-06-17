'''
    File name: imageUtil.py
    Author: Alexandre Heintzmann
    Date created: 26/03/2013
    Date last modified: 26/03/2013
    Python Version: 3.6.7
'''
import numpy as np
import cv2


def normalise(image):
    max = np.amax(image)
    min = np.amin(image)
    (n, m) = np.shape(image)
    for i in range(n):
        for j in range(m):
            image[i, j] = ((image[i, j] - min) / (max - min)) * 255
    return image


def resize(image, width):
    # initialize the dimensions of the image to be resized and
    # grab the image size
    dim = None
    (h, w) = image.shape[:2]

    r = width / float(w)
    dim = (width, int(h * r))

    # resize the image
    resized = cv2.resize(image, dim, interpolation = cv2.INTER_AREA)

    # return the resized image
    return resized
