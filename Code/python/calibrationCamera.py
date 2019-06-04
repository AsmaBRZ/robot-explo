import numpy as np
import cv2
import glob

# termination criteria
criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)

# prepare object points, like (0,0,0), (1,0,0), (2,0,0) ....,(6,5,0)
objp = np.zeros((7*7,3), np.float32)
objp[:,:2] = np.mgrid[0:7,0:7].T.reshape(-1,2)

# Arrays to store object points and image points from all the images.
objpoints = [] # 3d point in real world space
imgpoints = [] # 2d points in image plane.

images = glob.glob('*.jpg')

for i in range(2,10):
    fname = "cb"+str(i)+".jpg"
    img = cv2.imread(fname)
    gray = cv2.cvtColor(img,cv2.COLOR_BGR2GRAY)
    # Find the chess board corners
    ret, corners = cv2.findChessboardCorners(gray, (7,7),None)

    # If found, add object points, image points (after refining them)
    if ret == True:
        objpoints.append(objp)
        imgpoints.append(corners)
        corners2 = cv2.cornerSubPix(gray,corners,(7,7),(-1,-1),criteria)
        cv2.drawChessboardCorners(img, (6,6), corners2,ret)
        ret, mtx, dist, rvecs, tvecs = cv2.calibrateCamera(objpoints, imgpoints, gray.shape[::-1], None, None)
        np.savez("webcam_calibration_ouput", ret=ret, mtx=mtx, dist=dist, rvecs=rvecs, tvecs=tvecs)
        #ret, mtx, dist, rvecs, tvecs = cv2.calibrateCamera(objpoints, imgpoints, (600,800),None,None)
        # print(ret, "\n")
        # print(mtx, "\n")
        # print(dist, "\n")
        # print(rvecs, "\n")
        # print(tvecs, "\n")
        # Draw and display the corners
        cv2.imshow('img',img)
        cv2.waitKey(3000)

cv2.destroyAllWindows()