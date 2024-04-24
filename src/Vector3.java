class Vector3 {
    double x, y, z;

    Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    Vector3 scale(double scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }

    double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    Vector3 normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return new Vector3(x / length, y / length, z / length);
    }
}