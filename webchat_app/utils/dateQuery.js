export function todayStr() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function thisMonthStr() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export function thisYearStr() {
  return String(new Date().getFullYear());
}

export function monthRange(ym) {
  const [y, m] = ym.split('-').map(Number);
  const mm = String(m).padStart(2, '0');
  const last = new Date(y, m, 0).getDate();
  return {
    startDate: `${y}-${mm}-01`,
    endDate: `${y}-${mm}-${String(last).padStart(2, '0')}`,
    year: y,
    month: m,
  };
}

export function yearRange(year) {
  const y = Number(year) || new Date().getFullYear();
  return {
    year: y,
    startDate: `${y}-01-01`,
    endDate: `${y}-12-31`,
  };
}

export function defaultDateState() {
  return {
    dateMode: 'day',
    selectedDate: todayStr(),
    selectedMonth: thisMonthStr(),
    selectedYear: thisYearStr(),
    dateLabel: todayStr(),
  };
}

export function buildDateQuery(dateMode, selectedDate, selectedMonth, selectedYear) {
  if (dateMode === 'month') {
    const { startDate, endDate } = monthRange(selectedMonth || thisMonthStr());
    return {
      startDate,
      endDate,
      label: selectedMonth || thisMonthStr(),
    };
  }
  if (dateMode === 'year') {
    const y = selectedYear || thisYearStr();
    const { startDate, endDate } = yearRange(y);
    return {
      startDate,
      endDate,
      label: `${y}年`,
    };
  }
  const day = selectedDate || todayStr();
  return {
    startDate: day,
    endDate: day,
    label: day,
  };
}

export function applyDateQuery(params, dateMode, selectedDate, selectedMonth, selectedYear) {
  const query = buildDateQuery(dateMode, selectedDate, selectedMonth, selectedYear);
  params.startDate = query.startDate;
  params.endDate = query.endDate;
  return query;
}

export function periodPrefix(dateMode) {
  if (dateMode === 'month') return '当月';
  if (dateMode === 'year') return '当年';
  return '当日';
}

export function filterStatePatch(filterKey, filterIndex) {
  return { filterKey, filterIndex };
}

export function createDateQueryHandlers(reload) {
  return {
    onDateModeChange(e) {
      const mode = e.currentTarget.dataset.mode;
      if (mode === this.data.dateMode) return;
      const query = buildDateQuery(
        mode,
        this.data.selectedDate,
        this.data.selectedMonth,
        this.data.selectedYear,
      );
      this.setData({ dateMode: mode, dateLabel: query.label, page: 1 }, () => reload.call(this));
    },

    onDatePickChange(e) {
      const val = e.detail.value || '';
      const { dateMode } = this.data;
      const patch = { page: 1 };
      if (dateMode === 'day') {
        patch.selectedDate = val;
      } else if (dateMode === 'month') {
        patch.selectedMonth = val.length >= 7 ? val.slice(0, 7) : val;
      } else {
        patch.selectedYear = val.length >= 4 ? val.slice(0, 4) : val;
      }
      const query = buildDateQuery(
        dateMode,
        patch.selectedDate || this.data.selectedDate,
        patch.selectedMonth || this.data.selectedMonth,
        patch.selectedYear || this.data.selectedYear,
      );
      patch.dateLabel = query.label;
      this.setData(patch, () => reload.call(this));
    },
  };
}
