import pickle
import sys
from PIL import Image
from os import walk
import os
import PIL
import PIL.ImageOps
from os import walk
from PIL import Image

def copyImageFile(src, dest, dimen_multiplier):
	im = Image.open(src)
	(width, height) = im.size
	width = int(width * float(dimen_multiplier))
	height = int(height * float(dimen_multiplier))
	im2 = im.resize((width, height), resample=Image.ANTIALIAS)
	im2.save(dest)
	print("updated file: "+dest)

def getFileLastModificationDate(filename):
	return os.path.getsize(filename)

def fileHasChangedAccordingToDict(filename):
	global file_index
	if filename in file_index:
		return getFileLastModificationDate(filename) != file_index[filename]
	else:
		return True

def writeFileLastModificationDateToDict(filename):
	global file_index
	file_index[filename] = getFileLastModificationDate(filename)

index_file_name = "_files_index.txt"
file_index = {}
if os.path.exists(index_file_name):
	with open(index_file_name, 'rb') as handle:
		file_index = pickle.loads(handle.read())

for (dirpath, dirnames, filenames) in walk("./"):
	for filename in filenames:
		if filename.endswith(".svg"):
			if fileHasChangedAccordingToDict(filename):
				writeFileLastModificationDateToDict(filename)
				dest_filename = filename.replace(".svg", "-web.png")
				os.system('inkscape -z -f "'+filename+ '" -e "'+dest_filename+'" -h 512')
				if not filename.endswith("_c.svg"):
					image = Image.open(dest_filename)
					image.save(dest_filename)

for (dirpath, dirnames, filenames) in walk("./"):
	for filename in filenames:
		if filename.endswith("-web.png"):
			if fileHasChangedAccordingToDict(filename):
				writeFileLastModificationDateToDict(filename)
				dest_filename = filename.replace("-web.png", ".png")
				copyImageFile("./"+filename, "./res/drawable-xxhdpi/"+dest_filename, 0.1875)
				copyImageFile("./"+filename, "./res/drawable-xhdpi/"+dest_filename, 0.125)
				copyImageFile("./"+filename, "./res/drawable-hdpi/"+dest_filename, 0.09375)
				copyImageFile("./"+filename, "./res/drawable-mdpi/"+dest_filename, 0.0625)
	break

with open(index_file_name, 'wb') as handle:
	pickle.dump(file_index, handle)