package sagan.site.projects;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import org.hibernate.annotations.SortNatural;
import sagan.site.projects.support.SupportPolicy;
import sagan.site.projects.support.SupportTimeline;

@Embeddable
public class ProjectGenerationsInfo {

	/**
	 * Set of available {@link ProjectGeneration} sorted by their name.
	 */
	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@SortNatural
	private SortedSet<ProjectGeneration> generations = new TreeSet<>();

	/**
	 * Last modification date of the set of generations.
	 */
	private OffsetDateTime lastModified = LocalDateTime.now().atOffset(ZoneOffset.UTC);

	public SortedSet<ProjectGeneration> getGenerations() {
		return generations;
	}

	public void setGenerations(SortedSet<ProjectGeneration> generations) {
		this.generations = generations;
	}

	public OffsetDateTime getLastModified() {
		return lastModified;
	}

	public void setLastModified(OffsetDateTime lastModified) {
		this.lastModified = lastModified;
	}

	public void recordModification() {
		this.lastModified = LocalDateTime.now().atOffset(ZoneOffset.UTC);
	}

	public void computeSupportPolicyDates(SupportPolicy supportPolicy) {
		iterateOnGenerations((currentGeneration, nextGeneration) -> {
			LocalDate nextGenReleaseDate = nextGeneration != null ? nextGeneration.getInitialReleaseDate() : null;
			SupportTimeline currentGenTimeline = supportPolicy.calculateTimeline(currentGeneration.getInitialReleaseDate(), nextGenReleaseDate);
			currentGeneration.setOssSupportPolicyEndDate(currentGenTimeline.getOpenSourceSupport().getEndDate());
			currentGeneration.setCommercialSupportPolicyEndDate(currentGenTimeline.getCommercialSupport().getEndDate());
		});
	}

	private <T> void iterateOnGenerations(BiConsumer<ProjectGeneration, ProjectGeneration> consumer) {
		Iterator<ProjectGeneration> it = this.generations.iterator();
		if(!it.hasNext()) return;
		ProjectGeneration first = it.next();
		while(it.hasNext()) {
			ProjectGeneration next = it.next();
			consumer.accept(first, next);
			first = next;
		}
		consumer.accept(first, null);
	}
}
