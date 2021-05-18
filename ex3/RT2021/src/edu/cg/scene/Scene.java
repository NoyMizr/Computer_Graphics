package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Plain;
import edu.cg.scene.objects.Surface;

public class Scene {
    private String name = "scene";
    private int maxRecursionLevel = 1;
    private int antiAliasingFactor = 1; //gets the values of 1, 2 and 3
    private boolean renderRefractions = false;
    private boolean renderReflections = false;

    private PinholeCamera camera;
    private Vec ambient = new Vec(0.1, 0.1, 0.1); //white
    private Vec backgroundColor = new Vec(0, 0.5, 1); //blue sky
    private List<Light> lightSources = new LinkedList<>();
    private List<Surface> surfaces = new LinkedList<>();


    //MARK: initializers
    public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec, double distanceToPlain) {
        this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec, distanceToPlain);
        return this;
    }

    public Scene initCamera(PinholeCamera pinholeCamera) {
        this.camera = pinholeCamera;
        return this;
    }

    public Scene initAmbient(Vec ambient) {
        this.ambient = ambient;
        return this;
    }

    public Scene initBackgroundColor(Vec backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public Scene addLightSource(Light lightSource) {
        lightSources.add(lightSource);
        return this;
    }

    public Scene addSurface(Surface surface) {
        surfaces.add(surface);
        return this;
    }

    public Scene initMaxRecursionLevel(int maxRecursionLevel) {
        this.maxRecursionLevel = maxRecursionLevel;
        return this;
    }

    public Scene initAntiAliasingFactor(int antiAliasingFactor) {
        this.antiAliasingFactor = antiAliasingFactor;
        return this;
    }

    public Scene initName(String name) {
        this.name = name;
        return this;
    }

    public Scene initRenderRefractions(boolean renderRefractions) {
        this.renderRefractions = renderRefractions;
        return this;
    }

    public Scene initRenderReflections(boolean renderReflections) {
        this.renderReflections = renderReflections;
        return this;
    }

    //MARK: getters
    public String getName() {
        return name;
    }

    public int getFactor() {
        return antiAliasingFactor;
    }

    public int getMaxRecursionLevel() {
        return maxRecursionLevel;
    }

    public boolean getRenderRefractions() {
        return renderRefractions;
    }

    public boolean getRenderReflections() {
        return renderReflections;
    }

    @Override
    public String toString() {
        String endl = System.lineSeparator();
        return "Camera: " + camera + endl +
                "Ambient: " + ambient + endl +
                "Background Color: " + backgroundColor + endl +
                "Max recursion level: " + maxRecursionLevel + endl +
                "Anti aliasing factor: " + antiAliasingFactor + endl +
                "Light sources:" + endl + lightSources + endl +
                "Surfaces:" + endl + surfaces;
    }

    private transient ExecutorService executor = null;
    private transient Logger logger = null;

    // TODO: add your fields here with the transient keyword
    //  for example - private transient Object myField = null;


    private void initSomeFields(int imgWidth, int imgHeight, double planeWidth, Logger logger) {
        this.logger = logger;
        // TODO: initialize your fields that you added to this class here.
        //      Make sure your fields are declared with the transient keyword
    }


    public BufferedImage render(int imgWidth, int imgHeight, double planeWidth, Logger logger)
            throws InterruptedException, ExecutionException, IllegalArgumentException {

        initSomeFields(imgWidth, imgHeight, planeWidth, logger);

        BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        camera.initResolution(imgHeight, imgWidth, planeWidth);
        int nThreads = Runtime.getRuntime().availableProcessors();
        nThreads = nThreads < 2 ? 2 : nThreads;
        this.logger.log("Initialize executor. Using " + nThreads + " threads to render " + name);
        executor = Executors.newFixedThreadPool(nThreads);

        @SuppressWarnings("unchecked")
        Future<Color>[][] futures = (Future<Color>[][]) (new Future[imgHeight][imgWidth]);

        this.logger.log("Starting to shoot " +
                (imgHeight * imgWidth * antiAliasingFactor * antiAliasingFactor) +
                " rays over " + name);

        for (int y = 0; y < imgHeight; ++y)
            for (int x = 0; x < imgWidth; ++x)
                futures[y][x] = calcColor(x, y);

        this.logger.log("Done shooting rays.");
        this.logger.log("Wating for results...");

        for (int y = 0; y < imgHeight; ++y)
            for (int x = 0; x < imgWidth; ++x) {
                Color color = futures[y][x].get();
                img.setRGB(x, y, color.getRGB());
            }

        executor.shutdown();

        this.logger.log("Ray tracing of " + name + " has been completed.");

        executor = null;
        this.logger = null;

        return img;
    }

    private Future<Color> calcColor(int x, int y) {
        return executor.submit(() -> {
            Point pointOnScreen = camera.transform(x, y);
            Vec color = new Vec(0.0);

            Ray ray = new Ray(this.camera.getCameraPosition(), pointOnScreen);
            color = color.add(calcColor(ray, 0));

            if (antiAliasingFactor > 1) {
                for (int i = 0; i < antiAliasingFactor - 1; i++) {
                    double randomX = -1 + (Math.random() * 2);
                    double randomY = -1 + (Math.random() * 2);
                    Point randomPoint = new Point(pointOnScreen.x + randomX * 0.5 * camera.pixelWidth,
                            pointOnScreen.y + randomY * 0.5 * camera.pixelWidth, pointOnScreen.z);
                    Ray randomRay = new Ray(camera.getCameraPosition(), randomPoint);
                    color = color.add(calcColor(randomRay, 0));
                }

                color = color.mult((float) 1 / antiAliasingFactor);
            }

            return color.toColor();
        });
    }


    private Vec calcColor(Ray ray, int recursionLevel) {
        Hit minimalIntersection = findMinIntersection(ray);
        if (minimalIntersection == null) {
            return this.backgroundColor;
        }
        Surface intersectionSurface = minimalIntersection.getSurface();
        Point intersectionPoint = ray.getHittingPoint(minimalIntersection);
        Vec pixelColor = calcColorAtIntersectionPoint(ray, minimalIntersection, intersectionSurface, intersectionPoint);

        if (recursionLevel == maxRecursionLevel) {
            return pixelColor;
        }
        if (renderReflections && intersectionSurface.isReflecting()) {
            Vec kr = intersectionSurface.Kr();
            Ray reflectedRayR = getReflectedRay(ray, minimalIntersection, intersectionPoint);
            Vec reflectionColor = calcColor(reflectedRayR, recursionLevel + 1);
            pixelColor = pixelColor.add(reflectionColor.mult(kr));
        }
        if (renderRefractions && intersectionSurface.isTransparent()) {
            Vec kt = intersectionSurface.Kt();
            Ray refractedRayT = getRefractedRay(ray, minimalIntersection, intersectionPoint);
            Vec refractionColor = calcColor(refractedRayT, recursionLevel + 1);
            pixelColor = pixelColor.add(refractionColor.mult(kt));
        }
        return pixelColor;
    }

    private Hit findMinIntersection(Ray ray) {
        double minT = Double.POSITIVE_INFINITY;
        LinkedList<Hit> intersections = new LinkedList<>();
        for (Surface s : this.surfaces) {
            Hit surfaceIntersection = s.intersect(ray);
            if (surfaceIntersection != null) intersections.add(surfaceIntersection);
        }
        Hit minIntersection = null;
        for (int i = 0; i < intersections.size(); i++) {
            if (intersections.get(i).t() < minT) {
                minIntersection = intersections.get(i);
                minT = intersections.get(i).t();
            }
        }
        return minIntersection;
    }

    private Vec calcColorAtIntersectionPoint(Ray ray, Hit hit, Surface surface, Point point) {
        Vec color = new Vec(0);
        color = color.add(getAmbientReflection(surface));
        for (Light lightSource : this.lightSources) {
            Ray rayFromPointToLight = lightSource.rayToLight(point);
            Vec lightIntensity = lightSource.intensity(point, rayFromPointToLight);
            if (!isOccludedFromLight(lightSource, rayFromPointToLight)) {
                Vec diffuse = getDiffuseReflection(hit, rayFromPointToLight);
                Vec specular = getSpecularReflection(hit, ray, rayFromPointToLight);
                color = color.add(diffuse.add(specular).mult(lightIntensity));
            }
        }
        return color;
    }

    private Vec getAmbientReflection(Surface surface) {
        Vec ka = surface.Ka();
        return ka.mult(this.ambient);
    }

    private boolean isOccludedFromLight(Light lightSource, Ray rayToLight) {
        for (Surface s : this.surfaces) {
            if (lightSource.isOccludedBy(s, rayToLight)) {
                return true;
            }
        }
        return false;
    }

    private Vec getDiffuseReflection(Hit hit, Ray rayToLight) {
        Vec kd = hit.getSurface().Kd();
        Vec N = hit.getNormalToSurface().normalize();
        Vec L = rayToLight.direction().normalize();
        return kd.mult(N.dot(L));
    }

    private Vec getSpecularReflection(Hit hit, Ray rayFromViewer, Ray rayToLight) {
        double n = hit.getSurface().shininess();
        Vec ks = hit.getSurface().Ks();
        Vec V = rayFromViewer.direction().neg().normalize();
        Vec N = hit.getNormalToSurface().normalize();
        Vec LReflect = Ops.reflect(rayToLight.direction().neg().normalize(), N);
        double cosAlpha = V.dot(LReflect);
        return (cosAlpha < Ops.epsilon) ? new Vec(0) : ks.mult(Math.pow(cosAlpha, n));
    }

    private Ray getReflectedRay(Ray ray, Hit hit, Point hittingPoint) {
        Vec normalToSurface = hit.getNormalToSurface().normalize();
        Vec reflectDirection = Ops.reflect(ray.direction().normalize(), normalToSurface).normalize();
        return new Ray(hittingPoint, reflectDirection);
    }

    private Ray getRefractedRay(Ray ray, Hit hit, Point hittingPoint) {
        Vec normalToSurface = hit.getNormalToSurface().normalize();
        Vec N = hit.isWithinTheSurface() ? normalToSurface.neg() : normalToSurface;
        double n1 = hit.getSurface().n1(hit);
        double n2 = hit.getSurface().n2(hit);
        Vec refractDirection = Ops.refract(ray.direction().normalize(), N, n1, n2).normalize();
        return new Ray(hittingPoint, refractDirection);
    }

}
