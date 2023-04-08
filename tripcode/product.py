from itertools import product
from sys import argv

if __name__ == "__main__":
    if len(argv) > 2:
        repeat = int(argv[1])
        string = argv[2]

        with open("products.txt", "w") as file:
            for i in range(1, repeat+1):
                result = ("".join(x) for x in product(string, repeat=i))
                print(f"[{i}]", file=file)
                for num in result:
                    print(num, file=file)
                file.flush()
            print("ok")
    else:
        print("null")