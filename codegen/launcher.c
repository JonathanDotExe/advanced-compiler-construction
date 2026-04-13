#include <stdio.h>
#include <string.h>
#include <sys/mman.h>

char data[65536];

void Put(char ch) {
    printf("%c", ch);
}

void PutLn() {
    printf("\n");
}

int main(int argc, char* argv[])
{
    /* check arguments */
    if (argc != 2) {
        printf("usage: %s <binary file>\n", argv[0]);
        return -1;
    }

    /* load the code file */
    FILE* fin = fopen(argv[1], "rb");
    if (fin == NULL) {
        printf("couldn't open %s\n", argv[1]);
        return -2;
    }

    /* allocate executable memory using mmap (replaces VirtualAllocEx) */
    char* code = (char*) mmap(NULL, 1<<16,
                              PROT_READ | PROT_WRITE,
                              MAP_PRIVATE | MAP_ANONYMOUS,
                              -1, 0);

    if (code == MAP_FAILED) {
        // output error details
        perror("mmap failed");
        fclose(fin);
        return -3;
    }

    size_t size = fread(code, 1, 1<<16, fin);
    fclose(fin);

    printf("loaded %zu bytes\n", size);
    printf("executing...\n");

    /* setup the global data block */
    memset(data, 0, sizeof(data));
    ((void**)data)[0] = (void*)Put;
    ((void**)data)[1] = (void*)PutLn;

    // Switch to executable, remove write
    if (mprotect(code, size, PROT_READ | PROT_EXEC) == -1) {
        perror("mprotect failed");
        munmap(code, 1<<16);
        return -4;
    }

    // define start to be a function pointer to a function like void start(void* data) that is implemented by the code we just loaded
    void (*start)(void*) = (void (*)(void*))code;
    start(data);

    /* clean up */
    munmap(code, 1<<16);

    return 0;
}
