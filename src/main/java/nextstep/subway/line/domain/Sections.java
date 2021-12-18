package nextstep.subway.line.domain;

import nextstep.subway.station.domain.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Embeddable
public class Sections {
    private static final int FIRST_INDEX = 0;
    private static final int MINIMUM_SECTIONS_SIZE = 1;
    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    protected Sections() {
    }

    public Sections(Section section) {
        sections.add(section);
    }

    public List<Section> getValue() {
        return sections;
    }

    public List<Station> getStations() {
        if (sections.isEmpty()) {
            return Arrays.asList();
        }

        List<Station> stations = new ArrayList<>();
        Station downStation = findUpStation();
        stations.add(downStation);

        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                    .filter(section -> section.getUpStation().equals(finalDownStation))
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getDownStation();
            stations.add(downStation);
        }

        return stations;
    }

    public void addLineStations(Section section) {
        List<Station> stations = getStations();
        boolean isUpStationExisted = stations.stream().anyMatch(station -> station.equals(section.getUpStation()));
        boolean isDownStationExisted = stations.stream().anyMatch(station -> station.equals(section.getDownStation()));

        validateIsAllContainsStations(isUpStationExisted, isDownStationExisted);

        validateIsNotAllContainsStations(section, stations);

        if (isUpStationExisted) {
            addDownStation(section);
        }
        if (isDownStationExisted) {
            addUpStation(section);
        }

        sections.add(section);
    }

    private Station findUpStation() {
        Station downStation = sections.get(0).getUpStation();
        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                    .filter(section -> section.getDownStation().equals(finalDownStation))
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getUpStation();
        }

        return downStation;
    }

    private void validateIsAllContainsStations(boolean isUpStationExisted, boolean isDownStationExisted) {
        if (isUpStationExisted && isDownStationExisted) {
            throw new RuntimeException("이미 등록된 구간 입니다.");
        }
    }

    private void validateIsNotAllContainsStations(Section section, List<Station> stations) {
        if (!stations.isEmpty() && stations.stream().noneMatch(station -> station.equals(section.getUpStation())) &&
                stations.stream().noneMatch(station -> station.equals(section.getDownStation()))) {
            throw new RuntimeException("등록할 수 없는 구간 입니다.");
        }
    }

    private void addDownStation(Section sectionToAdd) {
        sections.stream()
                .filter(section -> section.getUpStation().equals(sectionToAdd.getUpStation()))
                .findFirst()
                .ifPresent(section -> section.updateUpStation(sectionToAdd.getDownStation(), sectionToAdd.getDistance()));
    }

    private void addUpStation(Section sectionToAdd) {
        sections.stream()
                .filter(section -> section.getDownStation().equals(sectionToAdd.getDownStation()))
                .findFirst()
                .ifPresent(section -> section.updateDownStation(sectionToAdd.getUpStation(), sectionToAdd.getDistance()));
    }

    public void removeLineStation(Station station) {
        if (sections.size() <= MINIMUM_SECTIONS_SIZE) {
            throw new RuntimeException();
        }
        List<Station> stations = getStations();

        Optional<Section> upLineStationSection = sections.stream()
                .filter(section -> section.getUpStation().equals(station))
                .findFirst();
        Optional<Section> downLineStationSection = sections.stream()
                .filter(section -> section.getDownStation().equals(station))
                .findFirst();

        if (!isFirstStation(station, stations) && !isLastStation(station, stations)) {
            removeBetweenSectionStation(station);
        }

        upLineStationSection.ifPresent(section -> sections.remove(section));
        downLineStationSection.ifPresent(section -> sections.remove(section));
    }

    private Line getLine() {
        return sections.get(FIRST_INDEX).getLine();
    }

    private boolean isFirstStation(Station station, List<Station> stations) {
        return stations.get(FIRST_INDEX).equals(station);
    }

    private boolean isLastStation(Station station, List<Station> stations) {
        return stations.get(stations.size()-1).equals(station);
    }

    private void removeBetweenSectionStation(Station station) {
        Section upLineStationSection = sections.stream()
                .filter(section -> section.getUpStation().equals(station))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("일치하는 구간이 없습니다."));
        Section downLineStationSection = sections.stream()
                .filter(section -> section.getDownStation().equals(station))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("일치하는 구간이 없습니다."));

        Station newUpStation = downLineStationSection.getUpStation();
        Station newDownStation = upLineStationSection.getDownStation();
        int newDistance = upLineStationSection.getDistance() + downLineStationSection.getDistance();
        sections.add(new Section(getLine(), newUpStation, newDownStation, newDistance));
    }
}
