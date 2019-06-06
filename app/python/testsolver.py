def getIntersectionPoint(l1u,l1v,l2u,l2v):
	# Equation de la droite : 
	if(l1v[0]!=l1u[0]):
		l1A = (l1v[1]-l1u[1])/(l1v[0]-l1u[0])
	else :
		l1A = 0	
	print("l1A ", l1A)
	l1B = l1u[1] - l1A*l1u[0]
	print("l1B ", l1B)
	if(l2v[0]-l2u[0]):
		l2A = (l2v[1]-l2u[1])/(l2v[0]-l2u[0])
	else :
		l2A = 0
	l2B = l2u[1] - l2A*l2u[0]
	print("l2A ", l2A)
	print("l2B ", l2B)
	# point d'intersection resolution
	a = np.array ( ( (-l1A, 1), (-l2A,1) ))
	b = np.array ( (l1B, l2B) )
	interx, intery = np.linalg.solve(a,b)
	return interx,intery



import numpy as np
print(getIntersectionPoint([568, 12], [794, 4], [441.80834197998047, 183.3110749721527], [442.95136528556856, 355.0486347144314]))
#([568, 12], [794, 4], [441.80834197998047, 183.3110749721527], [442.95136528556856, 355.0486347144314], 442.47378341876504, 283.2926661519099)
