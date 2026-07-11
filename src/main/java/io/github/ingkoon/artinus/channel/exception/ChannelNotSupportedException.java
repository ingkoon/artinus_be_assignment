package io.github.ingkoon.artinus.channel.exception;



public class ChannelNotSupportedException extends RuntimeException{
    public ChannelNotSupportedException() {
        throw new IllegalArgumentException("유효하지 않은 채널입니다.");
    }
}
