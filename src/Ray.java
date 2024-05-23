class Ray {
    Vector3 origin;
    Vector3 direction;

    Ray(Vector3 origin, Vector3 direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public Vector3 pointAtParameter(double t) {
        return origin.add(direction.scale(t));
    }
}