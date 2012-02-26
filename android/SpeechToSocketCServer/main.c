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


int main(int argc, char *argv[])/*{{{*/
{
    char buffer[BUFFER_SIZE];
    int fd;
    struct termios options;
    struct termios oldoptions;
    int baud = 9600;

    /* open the serial port */
    fd = open(SERIAL_DEVICE, O_RDWR | O_NOCTTY);// | O_NONBLOCK);
    if(fd == -1) {
        printf("Unable to open port %s\n", SERIAL_DEVICE);
        exit(EXIT_FAILURE);
    }

    tcgetattr(fd,&oldoptions);
    options = oldoptions;
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
    tcsetattr(fd,TCSANOW,&options);

    
    /* write a launch command of SpeechToSocketActivity */
    {
        char tmp[1] = {'f'};
        write(fd, tmp, 1);
    }

    while (1) {
        int numBytes;
        size_t s;
        printf("Wainting for read\n");
        s = read(fd, buffer, BUFFER_SIZE - 1);
        if (s > 0) {
            int n = s - 1;
            buffer[s] = '\0';
            for ( ; n >= 0; n--) {
                if (buffer[n] == '\n')
                    buffer[n] = '\0';
            }
            printf("%s\n", buffer);
        } else {
            break;
        }
    }
    close(fd);
    printf("Connection closed\n");

    return 0;
}/*}}}*/

/* Modeline for ViM {{{
 * vim:set ts=4:
 * vim600:fdm=marker fdl=0 fdc=3:
 * }}} */

