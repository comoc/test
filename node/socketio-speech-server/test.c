#include <stdio.h>

int main(void) {
    char buff[256];
    while (fgets(buff, 256, stdin) != NULL) {
        printf("CCCCCC: %s\n", buff);
    }
    return 0;
}
