package com.multi.runrunbackend.domain.course.util.route;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CoursePathProcessor
 * @since : 2025. 12. 23. Tuesday
 */
@Component
@Slf4j
public class CoursePathProcessor {

    private static final double OVERLAP_EPS = 1e-5;
    private static final double OFFSET_METER = 5.0;

    private static final double STORE_SIMPLIFY_TOLERANCE_M = 12.0;

    private static final double ROUTE_SIMPLIFY_TOLERANCE_M = 12.0;

    private static final double THUMB_SIMPLIFY_TOLERANCE_M = 25.0;

    private static final double LOOP_DIRECT_THRESHOLD_M = 15.0;
    private static final double LOOP_EXTRA_DISTANCE_M = 25.0;
    private static final int LOOP_LOOKAHEAD = 10;


    private static final double THUMB_MIN_SEGMENT_M = 3.0;


    public LineString simplifyForStore(LineString path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        List<double[]> coords = toCoordinateList(path);
        coords = pruneSmallLoops(coords);

        if (coords == null || coords.size() < 2) {
            return path;
        }

        double lengthM = cumulativeDistance(coords, 0, coords.size() - 1);

        // ✅ 정책: 10km 이하는 원본 유지, 단 포인트가 2000 초과면 2000으로 샘플링
        if (lengthM <= 10_000) {
            if (coords.size() > 2000) {
                List<double[]> sampled = sampleCoordinates(coords, 2000);
                return buildLineString(path, sampled);
            }
            return path;
        }

        if (coords.size() > 2000) {
            List<double[]> sampled = sampleCoordinates(coords, 2000);
            return buildLineString(path, sampled);
        }
        return path;
    }

    private LineString buildLineString(LineString original, List<double[]> coords) {
        if (original == null || coords == null || coords.size() < 2) {
            return original;
        }
        Coordinate[] arr = new Coordinate[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            double[] c = coords.get(i);
            arr[i] = new Coordinate(c[0], c[1]); // [lng, lat]
        }
        return original.getFactory().createLineString(arr);
    }

    private double pickStoreToleranceMeters(double lengthM) {
        // 짧은 코스일수록 더 촘촘하게(형태 유지)
        if (lengthM <= 500) {
            return 2.0;        // 0~0.5km
        }
        if (lengthM <= 2_000) {
            return 3.0;      // 0.5~2km
        }
        if (lengthM <= 10_000) {
            return 6.0;     // 2~10km
        }
        if (lengthM <= 30_000) {
            return 10.0;    // 10~30km
        }
        return STORE_SIMPLIFY_TOLERANCE_M;     // 30km~ (기존 12m)
    }

    private int pickMinStorePoints(double lengthM, int originalPoints) {
        // “짧은데 꺾임이 있는 코스”가 2점으로 줄지 않게 최소 포인트 수 보장
        int min;
        if (lengthM <= 1_000) {
            min = 20;        // 0~1km
        } else if (lengthM <= 5_000) {
            min = 60;   // 1~5km
        } else {
            min = 120;                        // 5km~
        }
        return Math.min(originalPoints, min);
    }

    // CoursePathProcessor.normalizeRoute(...)

    public List<double[]> normalizeRoute(List<double[]> coords) {
        if (coords == null) {
            return null;
        }

        List<double[]> mitigated = mitigateOverlap(coords);
        List<double[]> pruned = pruneSmallLoops(mitigated);

        if (pruned == null || pruned.size() < 2) {
            return pruned;
        }

        double lengthM = cumulativeDistance(pruned, 0, pruned.size() - 1);

        // ✅ 정책: 10km 이하는 원본 유지, 단 포인트가 2000 초과면 2000으로 샘플링
        if (lengthM <= 10_000) {
            if (pruned.size() > 2000) {
                return sampleCoordinates(pruned, 2000);
            }
            return pruned;
        }

        // ✅ 정책: 10km 초과는 포인트 상한 2000 적용(넘으면 샘플링)
        if (pruned.size() > 2000) {
            return sampleCoordinates(pruned, 2000);
        }
        return pruned;
    }

    public List<double[]> prepareThumbnailPath(LineString rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return List.of();
        }

        List<double[]> coords = toCoordinateList(rawPath);

        coords = removeMicroSegments(coords, THUMB_MIN_SEGMENT_M); // 노이즈 제거
        coords = expandLoopIfNeeded(coords);                      // 루프/왕복 벌림(시각적)
        coords = simplifyCoordinates(coords, THUMB_SIMPLIFY_TOLERANCE_M);
        coords = sampleCoordinates(coords, 180);

        return coords;
    }

    public List<double[]> toCoordinateList(LineString path) {
        Coordinate[] coordinates = path.getCoordinates();
        List<double[]> list = new ArrayList<>(coordinates.length);
        for (Coordinate coordinate : coordinates) {
            list.add(new double[]{coordinate.getX(), coordinate.getY()}); // [lng, lat]
        }
        return list;
    }

    public LineString simplifyLineString(LineString path, double toleranceMeters) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        List<double[]> coords = toCoordinateList(path);
        coords = pruneSmallLoops(coords);
        List<double[]> simplified = simplifyCoordinates(coords, toleranceMeters);
        if (simplified == null || simplified.size() < 2) {
            return path;
        }
        Coordinate[] arr = new Coordinate[simplified.size()];
        for (int i = 0; i < simplified.size(); i++) {
            double[] c = simplified.get(i);
            arr[i] = new Coordinate(c[0], c[1]);
        }
        return path.getFactory().createLineString(arr);
    }

    public List<double[]> sampleCoordinates(List<double[]> coords, int maxCount) {
        if (coords == null || coords.size() <= maxCount) {
            return coords;
        }
        List<double[]> sampled = new ArrayList<>(maxCount);
        double step = (double) (coords.size() - 1) / (maxCount - 1);
        for (int i = 0; i < maxCount; i++) {
            int index = (int) Math.round(i * step);
            sampled.add(coords.get(Math.min(index, coords.size() - 1)));
        }
        return sampled;
    }

    private List<double[]> expandLoopIfNeeded(List<double[]> coords) {
        if (coords == null || coords.size() < 10) {
            return coords;
        }

        double[] start = coords.get(0);
        double[] end = coords.get(coords.size() - 1);

        if (distanceMeters(start, end) < 20) { // 루프 판단(대충)
            List<double[]> expanded = new ArrayList<>();
            expanded.add(start);

            for (int i = 1; i < coords.size(); i++) {
                double[] prev = coords.get(i - 1);
                double[] curr = coords.get(i);

                double dx = curr[0] - prev[0];
                double dy = curr[1] - prev[1];
                double len = Math.sqrt(dx * dx + dy * dy);

                if (len > 0) {
                    double offset = 5 / 111320.0; // 약 5m
                    double ox = -dy / len * offset;
                    double oy = dx / len * offset;
                    expanded.add(new double[]{curr[0] + ox, curr[1] + oy});
                } else {
                    expanded.add(curr);
                }
            }
            return expanded;
        }
        return coords;
    }

    private List<double[]> removeMicroSegments(List<double[]> coords, double minMeters) {
        if (coords == null || coords.size() < 2) {
            return coords;
        }

        List<double[]> result = new ArrayList<>();
        result.add(coords.get(0));

        for (int i = 1; i < coords.size(); i++) {
            if (distanceMeters(result.get(result.size() - 1), coords.get(i)) >= minMeters) {
                result.add(coords.get(i));
            }
        }
        return result;
    }

    private List<double[]> mitigateOverlap(List<double[]> coords) {
        if (coords == null || coords.size() < 3) {
            return coords;
        }
        List<double[]> adjusted = new ArrayList<>(coords.size() + 4);
        for (int i = 0; i < coords.size(); i++) {
            double[] current = coords.get(i).clone();
            if (!adjusted.isEmpty()) {
                double[] prev = adjusted.get(adjusted.size() - 1);
                if (almostSame(prev, current)) {
                    current[0] += 1e-5;
                    current[1] += 1e-5;
                }
            }

            if (adjusted.size() >= 2) {
                double[] prev = adjusted.get(adjusted.size() - 1);
                double[] prevPrev = adjusted.get(adjusted.size() - 2);
                if (almostSame(prevPrev, current)) {
                    double[] bulged = bulgePoint(prevPrev, prev, OFFSET_METER);
                    if (bulged != null) {
                        adjusted.add(bulged);
                    }
                }
            }
            adjusted.add(current);
        }
        return adjusted;
    }

    private double[] bulgePoint(double[] start, double[] middle, double offsetMeter) {
        double dx = middle[0] - start[0];
        double dy = middle[1] - start[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < OVERLAP_EPS) {
            return null;
        }
        double offsetDeg = offsetMeter / 111320.0;
        double ox = -dy / len * offsetDeg;
        double oy = dx / len * offsetDeg;
        return new double[]{middle[0] + ox, middle[1] + oy};
    }

    private boolean almostSame(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < OVERLAP_EPS && Math.abs(a[1] - b[1]) < OVERLAP_EPS;
    }

    private List<double[]> pruneSmallLoops(List<double[]> coords) {
        if (coords == null || coords.size() < 4) {
            return coords;
        }
        List<double[]> pruned = new ArrayList<>();
        int n = coords.size();
        int i = 0;
        pruned.add(coords.get(0));

        while (i < n - 1) {
            int skipTo = -1;
            int limit = Math.min(n - 1, i + LOOP_LOOKAHEAD);
            for (int j = i + 2; j <= limit; j++) {
                double direct = distanceMeters(coords.get(i), coords.get(j));
                if (direct <= LOOP_DIRECT_THRESHOLD_M) {
                    double pathLen = cumulativeDistance(coords, i, j);
                    if (pathLen - direct >= LOOP_EXTRA_DISTANCE_M) {
                        skipTo = j;
                        break;
                    }
                }
            }
            if (skipTo != -1) {
                i = skipTo;
            } else {
                i++;
            }
            pruned.add(coords.get(i));
        }
        return pruned;
    }

    private double cumulativeDistance(List<double[]> coords, int start, int end) {
        double sum = 0;
        for (int k = start + 1; k <= end; k++) {
            sum += distanceMeters(coords.get(k - 1), coords.get(k));
        }
        return sum;
    }

    public List<double[]> simplifyCoordinates(List<double[]> coords, double toleranceMeters) {
        if (coords == null || coords.size() <= 2 || toleranceMeters <= 0) {
            return coords;
        }

        int n = coords.size();
        double originLat = coords.get(0)[1];
        double originLng = coords.get(0)[0];
        double cosLat = Math.cos(Math.toRadians(originLat));

        double[][] projected = new double[n][2];
        for (int i = 0; i < n; i++) {
            double lng = coords.get(i)[0];
            double lat = coords.get(i)[1];
            double x = (lng - originLng) * 111320.0 * cosLat;
            double y = (lat - originLat) * 110540.0;
            projected[i][0] = x;
            projected[i][1] = y;
        }

        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;
        double toleranceSq = toleranceMeters * toleranceMeters;
        simplifySection(projected, 0, n - 1, keep, toleranceSq);

        List<double[]> simplified = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                simplified.add(coords.get(i));
            }
        }
        return simplified.size() >= 2 ? simplified : coords;
    }

    private void simplifySection(double[][] points, int start, int end, boolean[] keep,
        double toleranceSq) {
        if (end <= start + 1) {
            return;
        }

        double maxDist = 0;
        int index = -1;

        for (int i = start + 1; i < end; i++) {
            double dist = distancePointToSegmentSquared(points[i], points[start], points[end]);
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }

        if (index != -1 && maxDist > toleranceSq) {
            keep[index] = true;
            simplifySection(points, start, index, keep, toleranceSq);
            simplifySection(points, index, end, keep, toleranceSq);
        }
    }

    private double distancePointToSegmentSquared(double[] p, double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        if (dx == 0 && dy == 0) {
            dx = p[0] - a[0];
            dy = p[1] - a[1];
            return dx * dx + dy * dy;
        }
        double t = ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double projX = a[0] + t * dx;
        double projY = a[1] + t * dy;
        double diffX = p[0] - projX;
        double diffY = p[1] - projY;
        return diffX * diffX + diffY * diffY;
    }

    public double distanceMeters(double[] a, double[] b) {
        double R = 6371000.0;
        double dLat = Math.toRadians(b[1] - a[1]);
        double dLng = Math.toRadians(b[0] - a[0]);
        double lat1 = Math.toRadians(a[1]);
        double lat2 = Math.toRadians(b[1]);
        double sinLat = Math.sin(dLat / 2);
        double sinLng = Math.sin(dLng / 2);
        double c = sinLat * sinLat + sinLng * sinLng * Math.cos(lat1) * Math.cos(lat2);
        return 2 * R * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c));
    }
}