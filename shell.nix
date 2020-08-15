with import <nixpkgs> {};

stdenv.mkDerivation {
  name = "shut-the-box";
  buildInputs = [
    clojure
    jdk12
    nodejs-12_x
    unzip
    zip
  ];
}
