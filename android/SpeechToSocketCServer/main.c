#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <fcntl.h>
//#include <errno.h>
#include <termios.h>
#include <unistd.h>
//#include <pthread.h>

#if !defined(__APPLE__)  && !defined(linux)
#error This program can be compiled on Mac or Linux.
#endif


#define SERIAL_DEVICE "/dev/tty.NexusOne-SpeechServer"
#define BUFFER_SIZE 513
    
int open_serial(struct termios *oldoptions)
{
    int fd;
    struct termios options;
    int baud = 9600;
    
    /* open the serial port */
    fd = open(SERIAL_DEVICE, O_RDWR | O_NOCTTY);// | O_NONBLOCK);
    if(fd == -1) {
        return fd;
    }

    tcgetattr(fd, oldoptions);
    options = *oldoptions;
    switch(baud) {
    case 300:
        cfsetispeed(&options,B300);
        cfsetospeed(&options,B300);
        break;
    case 1200:
        cfsetispeed(&options,B1200);
        cfsetospeed(&options,B1200);
        break;
    case 2400:
        cfsetispeed(&options,B2400);
        cfsetospeed(&options,B2400);
        break;
    case 4800:
        cfsetispeed(&options,B4800);
        cfsetospeed(&options,B4800);
        break;
    case 9600:
        cfsetispeed(&options,B9600);
        cfsetospeed(&options,B9600);
        break;
    case 14400:
        cfsetispeed(&options,B14400);
        cfsetospeed(&options,B14400);
        break;
    case 19200:
        cfsetispeed(&options,B19200);
        cfsetospeed(&options,B19200);
        break;
    case 28800:
        cfsetispeed(&options,B28800);
        cfsetospeed(&options,B28800);
        break;
    case 38400:
        cfsetispeed(&options,B38400);
        cfsetospeed(&options,B38400);
        break;
    case 57600:
        cfsetispeed(&options,B57600);
        cfsetospeed(&options,B57600);
        break;
    case 115200:
        cfsetispeed(&options,B115200);
        cfsetospeed(&options,B115200);
        break;
    default:
        cfsetispeed(&options,B9600);
        cfsetospeed(&options,B9600);
        break;
    }

    options.c_cflag |= (CLOCAL | CREAD);
    options.c_cflag &= ~PARENB;
    options.c_cflag &= ~CSTOPB;
    options.c_cflag &= ~CSIZE;
    options.c_cflag |= CS8;
    tcsetattr(fd, TCSANOW, &options);
    return fd;
}

int main(int argc, char *argv[])/*{{{*/
{
    char buffer[BUFFER_SIZE];
    struct termios oldoptions;
    int fd = open_serial(&oldoptions);
    if (fd == -1) {
        printf("Unable to open port %s\n", SERIAL_DEVICE);
        exit(EXIT_FAILURE);
    }

    /* write a launch command of SpeechToSocketActivity */
    {
        char tmp[] = "f";
        write(fd, tmp, sizeof(tmp));
    }
        
    ssize_t s;
    char c; 
    int m; 

    while (1) {
        m = 0;

        /* read until '\n' */
        printf("Wainting for read\n");
        while (1) {
            s = read(fd, &c, 1);
            if (s < 0) {
                break;
            } else if (s > 0) {
                if (c == '\n') {
                    buffer[m] = '\0';
                    break;
                } else {
                    buffer[m] = c;
                    m++;
                    if (m >= BUFFER_SIZE - 1) {
                        buffer[m] = '\0';
                        break;
                    }
                }
            }
        }

        /* print the result */
        if (m > 0) {
            printf("%s\n", buffer);
        }

        if (s < 0) {
            break;
        }
    }
    tcsetattr(fd, TCSANOW, &oldoptions);
    close(fd);
    printf("Connection closed\n");

    return 0;
}/*}}}*/

/* Modeline for ViM {{{
 * vim:set ts=4:
 * vim600:fdm=marker fdl=0 fdc=3:
 * }}} */

