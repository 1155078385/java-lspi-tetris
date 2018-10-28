import matplotlib.pyplot as plt

FILE = 'train.txt'

train = []
test = []
with open(FILE) as f:
	for line in f:
		if 'Average training score:' in line:
			v = line.split(' ')
			train.append(float(v[3]))
		elif 'Average testing score:' in line:
			v = line.split(' ')
			test.append(float(v[3]))

plt.plot([i for i in range(len(train))],train)
plt.plot([i for i in range(len(test))],test)
plt.ylabel('lines cleared')
plt.xlabel('iteration')
plt.legend(['train', 'test'], loc='upper left')
plt.show()
