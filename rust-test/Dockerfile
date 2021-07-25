FROM rust:1.53.0 as build

# Name should be the same as name in Cargo.toml
ENV NAME=getindata-linserver-rust-test

WORKDIR /usr/src

# Download the target for static linking.
RUN rustup target add x86_64-unknown-linux-musl

RUN cargo new ${NAME}
WORKDIR /usr/src/${NAME}
COPY Cargo.toml  ./
# Copy the source
COPY src ./src

RUN cargo install --target x86_64-unknown-linux-musl --path .

WORKDIR /usr/src
RUN cp /usr/local/cargo/bin/${NAME} result
COPY 20_000_mil_podmorskiej_zeglugi.txt .
#ENV ROCKET_ADDRESS=0.0.0.0
#EXPOSE 8080
#CMD ["./result"]

# Copy the statically-linked binary into a scratch container.
FROM scratch
#FROM alpine:latest
COPY --from=build /usr/src/result .
COPY --from=build /usr/src/20_000_mil_podmorskiej_zeglugi.txt .
ENV ROCKET_ADDRESS=0.0.0.0
EXPOSE 8080
CMD ["./result"]

