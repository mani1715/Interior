'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  FolderKanban,
  Plus,
  Search,
  Filter,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  ExternalLink,
  Archive,
  RotateCcw,
  Sparkles,
  CheckCircle2,
  Clock,
  Eye,
  EyeOff,
  Star,
  MapPin,
  Calendar,
  X,
  AlertCircle,
  Loader2,
} from 'lucide-react';
import {
  ProjectSummaryDto,
  ProjectStatus,
  ProjectCategory,
  VisibilityStatus,
  CANONICAL_CATEGORIES,
} from '@/lib/projects/types';
import {
  fetchProjects,
  createProject,
  reorderProjects,
  archiveProject,
  restoreProject,
} from '@/lib/projects/api';

export default function ProjectsListPage() {
  const router = useRouter();

  const [projects, setProjects] = useState<ProjectSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Search
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [visibilityFilter, setVisibilityFilter] = useState<string>('ALL');
  const [includeArchived, setIncludeArchived] = useState(false);

  // Create Modal State
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [newTitle, setNewTitle] = useState('');
  const [newCategory, setNewCategory] = useState<ProjectCategory>('COMPLETE_HOME_INTERIOR');
  const [newShortDesc, setNewShortDesc] = useState('');
  const [newCity, setNewCity] = useState('');
  const [newState, setNewState] = useState('');

  // Reorder loading state
  const [reordering, setReordering] = useState(false);

  const loadProjects = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchProjects({
        includeArchived: true,
      });
      setProjects(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  // Filtered projects
  const filteredProjects = useMemo(() => {
    return projects.filter((p) => {
      if (!includeArchived && p.projectStatus === 'ARCHIVED') return false;
      if (statusFilter !== 'ALL' && p.projectStatus !== statusFilter) return false;
      if (categoryFilter !== 'ALL' && p.categoryCode !== categoryFilter) return false;
      if (visibilityFilter !== 'ALL' && p.visibilityStatus !== visibilityFilter) return false;
      if (searchTerm.trim()) {
        const q = searchTerm.toLowerCase();
        const matchesTitle = p.title.toLowerCase().includes(q);
        const matchesCategory = p.categoryDisplayName.toLowerCase().includes(q);
        const matchesCity = p.city ? p.city.toLowerCase().includes(q) : false;
        const matchesState = p.state ? p.state.toLowerCase().includes(q) : false;
        if (!matchesTitle && !matchesCategory && !matchesCity && !matchesState) {
          return false;
        }
      }
      return true;
    });
  }, [projects, includeArchived, statusFilter, categoryFilter, visibilityFilter, searchTerm]);

  // Metrics
  const metrics = useMemo(() => {
    const total = projects.filter((p) => p.projectStatus !== 'ARCHIVED').length;
    const ready = projects.filter((p) => p.projectStatus === 'READY').length;
    const draft = projects.filter((p) => p.projectStatus === 'DRAFT').length;
    const inPortfolio = projects.filter(
      (p) => p.projectStatus !== 'ARCHIVED' && p.visibilityStatus === 'PORTFOLIO'
    ).length;
    return { total, ready, draft, inPortfolio };
  }, [projects]);

  // Quick Reorder
  const handleMove = async (index: number, direction: 'up' | 'down') => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= filteredProjects.length) return;

    try {
      setReordering(true);
      const reordered = [...filteredProjects];
      const temp = reordered[index];
      reordered[index] = reordered[targetIndex];
      reordered[targetIndex] = temp;

      const orderedIds = reordered.map((p) => p.id);
      const updated = await reorderProjects(orderedIds);
      setProjects(updated);
    } catch (err: any) {
      alert(err?.message || 'Failed to reorder projects');
    } finally {
      setReordering(false);
    }
  };

  // Archive / Restore
  const handleToggleArchive = async (p: ProjectSummaryDto) => {
    try {
      if (p.projectStatus === 'ARCHIVED') {
        await restoreProject(p.id, p.version);
      } else {
        if (!confirm(`Are you sure you want to archive "${p.title}"? It will be removed from your portfolio.`)) {
          return;
        }
        await archiveProject(p.id, p.version);
      }
      await loadProjects();
    } catch (err: any) {
      alert(err?.message || 'Action failed');
    }
  };

  // Handle Create Project
  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || newTitle.trim().length < 3) {
      setCreateError('Title must be at least 3 characters');
      return;
    }

    try {
      setCreateLoading(true);
      setCreateError(null);
      const created = await createProject({
        title: newTitle.trim(),
        categoryCode: newCategory,
        shortDescription: newShortDesc.trim() || null,
        city: newCity.trim() || null,
        state: newState.trim() || null,
      });

      setIsCreateOpen(false);
      router.push(`/workspace/projects/${created.id}`);
    } catch (err: any) {
      setCreateError(err?.message || 'Failed to create project');
    } finally {
      setCreateLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#1F1F1F]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Top Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-sand-200/80 pb-6">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-semibold tracking-wider uppercase text-bronze-700">
                Work CMS
              </span>
              <span className="text-xs text-charcoal-400">•</span>
              <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                <CheckCircle2 className="w-3 h-3" /> Ready
              </span>
            </div>
            <h1 className="font-serif text-2xl sm:text-3xl font-normal text-charcoal-900 tracking-tight">
              Interior Project Stories
            </h1>
            <p className="text-xs sm:text-sm text-charcoal-600 mt-1 max-w-2xl">
              Create, organize, and curate real interior projects that feed your professional portfolio and client presentations.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Link
              href="/workspace/portfolio"
              className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-medium rounded-xl border border-sand-300 bg-white hover:bg-sand-50 text-charcoal-800 transition-colors shadow-sm"
            >
              <Eye className="w-3.5 h-3.5 text-bronze-700" />
              <span>Portfolio View</span>
            </Link>

            <button
              onClick={() => {
                setNewTitle('');
                setNewCategory('COMPLETE_HOME_INTERIOR');
                setNewShortDesc('');
                setNewCity('');
                setNewState('');
                setCreateError(null);
                setIsCreateOpen(true);
              }}
              className="inline-flex items-center gap-2 px-4 py-2 text-xs font-medium rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white transition-colors shadow-sm"
            >
              <Plus className="w-4 h-4 text-bronze-300" />
              <span>New Project</span>
            </button>
          </div>
        </div>

        {/* Metrics Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
            <div className="flex items-center justify-between text-xs text-charcoal-500 mb-1">
              <span>Total Active</span>
              <FolderKanban className="w-4 h-4 text-bronze-600" />
            </div>
            <div className="text-2xl font-serif text-charcoal-900">{metrics.total}</div>
            <div className="text-[11px] text-charcoal-400 mt-0.5">Projects in your studio</div>
          </div>

          <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
            <div className="flex items-center justify-between text-xs text-charcoal-500 mb-1">
              <span>Ready Stories</span>
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            </div>
            <div className="text-2xl font-serif text-charcoal-900">{metrics.ready}</div>
            <div className="text-[11px] text-charcoal-400 mt-0.5">Complete details validated</div>
          </div>

          <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
            <div className="flex items-center justify-between text-xs text-charcoal-500 mb-1">
              <span>Drafts</span>
              <Clock className="w-4 h-4 text-amber-600" />
            </div>
            <div className="text-2xl font-serif text-charcoal-900">{metrics.draft}</div>
            <div className="text-[11px] text-charcoal-400 mt-0.5">Missing summary or location</div>
          </div>

          <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm">
            <div className="flex items-center justify-between text-xs text-charcoal-500 mb-1">
              <span>In Portfolio</span>
              <Star className="w-4 h-4 text-bronze-700" />
            </div>
            <div className="text-2xl font-serif text-charcoal-900">{metrics.inPortfolio}</div>
            <div className="text-[11px] text-charcoal-400 mt-0.5">Visible on live portfolio</div>
          </div>
        </div>

        {/* Filters and Controls */}
        <div className="bg-white border border-sand-200 rounded-xl p-4 shadow-sm space-y-3">
          <div className="flex flex-col md:flex-row gap-3">
            {/* Search */}
            <div className="relative flex-1">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-charcoal-400" />
              <input
                type="text"
                placeholder="Search projects by title, category, city..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/50 text-charcoal-900 placeholder:text-charcoal-400"
              />
              {searchTerm && (
                <button
                  onClick={() => setSearchTerm('')}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-charcoal-400 hover:text-charcoal-600"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Status Filter */}
            <div className="flex items-center gap-2">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="text-xs py-2 px-2.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-700"
              >
                <option value="ALL">All Statuses</option>
                <option value="READY">Ready Only</option>
                <option value="DRAFT">Draft Only</option>
                <option value="ARCHIVED">Archived Only</option>
              </select>

              {/* Category Filter */}
              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="text-xs py-2 px-2.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-700"
              >
                <option value="ALL">All Categories</option>
                {CANONICAL_CATEGORIES.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.label}
                  </option>
                ))}
              </select>

              {/* Visibility Filter */}
              <select
                value={visibilityFilter}
                onChange={(e) => setVisibilityFilter(e.target.value)}
                className="text-xs py-2 px-2.5 rounded-lg border border-sand-300 bg-white text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-700"
              >
                <option value="ALL">All Visibility</option>
                <option value="PORTFOLIO">Portfolio</option>
                <option value="PRIVATE">Private</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between text-xs text-charcoal-500 pt-2 border-t border-sand-100">
            <div className="flex items-center gap-2">
              <label className="inline-flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={includeArchived}
                  onChange={(e) => setIncludeArchived(e.target.checked)}
                  className="rounded border-sand-300 text-bronze-700 focus:ring-bronze-700"
                />
                <span>Include archived projects</span>
              </label>
            </div>

            <div>
              Showing <span className="font-semibold text-charcoal-800">{filteredProjects.length}</span> of{' '}
              <span className="font-semibold text-charcoal-800">{projects.length}</span> projects
            </div>
          </div>
        </div>

        {/* Error Notification */}
        {error && (
          <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
            <button
              onClick={loadProjects}
              className="px-2 py-1 bg-white border border-red-200 rounded text-red-800 font-medium hover:bg-red-50"
            >
              Retry
            </button>
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="py-20 text-center space-y-3">
            <Loader2 className="w-8 h-8 text-bronze-700 animate-spin mx-auto" />
            <p className="text-xs text-charcoal-500">Loading your project stories...</p>
          </div>
        ) : filteredProjects.length === 0 ? (
          /* Empty State */
          <div className="bg-white border border-sand-200 rounded-2xl p-12 text-center max-w-lg mx-auto shadow-sm">
            <div className="w-12 h-12 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center mx-auto mb-4">
              <FolderKanban className="w-6 h-6" />
            </div>
            {projects.length === 0 ? (
              <>
                <h3 className="font-serif text-lg text-charcoal-900 mb-1">
                  Start Your First Project Story
                </h3>
                <p className="text-xs text-charcoal-500 leading-relaxed mb-6">
                  Add completed residential interiors, modular kitchens, or turnkey commercial spaces. You can save early drafts and refine specifications anytime.
                </p>
                <button
                  onClick={() => setIsCreateOpen(true)}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors shadow-sm"
                >
                  <Plus className="w-4 h-4 text-bronze-300" />
                  <span>Create Project</span>
                </button>
              </>
            ) : (
              <>
                <h3 className="font-serif text-lg text-charcoal-900 mb-1">
                  No projects match your filter
                </h3>
                <p className="text-xs text-charcoal-500 leading-relaxed mb-6">
                  Try adjusting search keywords, clearing status filters, or including archived projects.
                </p>
                <button
                  onClick={() => {
                    setSearchTerm('');
                    setStatusFilter('ALL');
                    setCategoryFilter('ALL');
                    setVisibilityFilter('ALL');
                    setIncludeArchived(true);
                  }}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-sand-300 text-charcoal-800 text-xs font-medium hover:bg-sand-50 transition-colors"
                >
                  <span>Reset All Filters</span>
                </button>
              </>
            )}
          </div>
        ) : (
          /* Projects Grid / List */
          <div className="space-y-3">
            {filteredProjects.map((p, index) => {
              const isArchived = p.projectStatus === 'ARCHIVED';
              const isReady = p.projectStatus === 'READY';

              return (
                <div
                  key={p.id}
                  className={`group bg-white border rounded-xl p-4 sm:p-5 transition-all shadow-sm hover:border-sand-300 ${
                    isArchived ? 'opacity-65 border-dashed border-sand-300 bg-sand-50/40' : 'border-sand-200'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    {/* Left: Reorder Controls + Info */}
                    <div className="flex items-start sm:items-center gap-3">
                      {/* Reorder Buttons */}
                      <div className="flex flex-col items-center justify-center gap-1 pr-2 border-r border-sand-200">
                        <button
                          disabled={reordering || index === 0}
                          onClick={() => handleMove(index, 'up')}
                          className="p-1 rounded hover:bg-sand-100 text-charcoal-400 hover:text-charcoal-800 disabled:opacity-30 disabled:hover:bg-transparent"
                          title="Move up in portfolio order"
                        >
                          <ArrowUp className="w-3.5 h-3.5" />
                        </button>
                        <span className="text-[10px] font-mono text-charcoal-400">
                          {index + 1}
                        </span>
                        <button
                          disabled={reordering || index === filteredProjects.length - 1}
                          onClick={() => handleMove(index, 'down')}
                          className="p-1 rounded hover:bg-sand-100 text-charcoal-400 hover:text-charcoal-800 disabled:opacity-30 disabled:hover:bg-transparent"
                          title="Move down in portfolio order"
                        >
                          <ArrowDown className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {/* Project Meta */}
                      <div className="space-y-1.5">
                        <div className="flex flex-wrap items-center gap-2">
                          <Link
                            href={`/workspace/projects/${p.id}`}
                            className="font-serif text-base sm:text-lg font-medium text-charcoal-900 hover:text-bronze-700 transition-colors"
                          >
                            {p.title}
                          </Link>

                          {/* Status Pill */}
                          {isReady ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                              <CheckCircle2 className="w-3 h-3" /> Ready
                            </span>
                          ) : isArchived ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-charcoal-600 bg-sand-100 px-2 py-0.5 rounded-full border border-sand-300">
                              <Archive className="w-3 h-3" /> Archived
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-amber-800 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                              <Clock className="w-3 h-3" /> Draft
                            </span>
                          )}

                          {/* Visibility Pill */}
                          {p.visibilityStatus === 'PORTFOLIO' ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-charcoal-700 bg-sand-100 px-2 py-0.5 rounded-full border border-sand-200">
                              <Eye className="w-3 h-3 text-bronze-700" /> In Portfolio
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-charcoal-500 bg-sand-50 px-2 py-0.5 rounded-full border border-sand-200">
                              <EyeOff className="w-3 h-3 text-charcoal-400" /> Private
                            </span>
                          )}

                          {/* Featured Star */}
                          {p.featured && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-bronze-800 bg-bronze-50 px-2 py-0.5 rounded-full border border-bronze-200">
                              <Star className="w-3 h-3 fill-bronze-700 text-bronze-700" /> Featured
                            </span>
                          )}
                        </div>

                        {/* Subline: Category & Styles & Location */}
                        <div className="flex flex-wrap items-center gap-3 text-xs text-charcoal-500">
                          <span className="font-medium text-charcoal-700">
                            {p.categoryDisplayName}
                          </span>

                          {(p.city || p.state) && (
                            <span className="inline-flex items-center gap-1 text-charcoal-500">
                              <MapPin className="w-3 h-3 text-bronze-600" />
                              {[p.city, p.state].filter(Boolean).join(', ')}
                            </span>
                          )}

                          {p.completionYear && (
                            <span className="inline-flex items-center gap-1 text-charcoal-500">
                              <Calendar className="w-3 h-3 text-charcoal-400" />
                              {p.completionYear}
                            </span>
                          )}

                          {p.styleDisplayNames?.length > 0 && (
                            <div className="flex items-center gap-1.5">
                              {p.styleDisplayNames.slice(0, 2).map((s) => (
                                <span
                                  key={s}
                                  className="text-[10px] bg-sand-100/70 text-charcoal-600 px-1.5 py-0.5 rounded border border-sand-200"
                                >
                                  {s}
                                </span>
                              ))}
                              {p.styleDisplayNames.length > 2 && (
                                <span className="text-[10px] text-charcoal-400">
                                  +{p.styleDisplayNames.length - 2}
                                </span>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Right: Actions */}
                    <div className="flex items-center gap-2 pl-9 sm:pl-0">
                      <Link
                        href={`/workspace/projects/${p.id}`}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-sand-300 bg-white hover:bg-sand-50 text-charcoal-800 text-xs font-medium transition-colors"
                      >
                        <span>Edit Details</span>
                      </Link>

                      <button
                        onClick={() => handleToggleArchive(p)}
                        className="p-1.5 rounded-lg border border-sand-200 hover:border-sand-300 text-charcoal-500 hover:text-charcoal-900 bg-white hover:bg-sand-50 transition-colors"
                        title={isArchived ? 'Restore to draft' : 'Archive project'}
                      >
                        {isArchived ? (
                          <RotateCcw className="w-3.5 h-3.5 text-emerald-700" />
                        ) : (
                          <Archive className="w-3.5 h-3.5 text-charcoal-500" />
                        )}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Create Project Modal */}
      {isCreateOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-charcoal-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-sand-200 relative space-y-4">
            <div className="flex items-center justify-between border-b border-sand-200 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-sand-100 text-bronze-700 flex items-center justify-center">
                  <FolderKanban className="w-4 h-4" />
                </div>
                <h3 className="font-serif text-lg text-charcoal-900">New Project Story</h3>
              </div>
              <button
                onClick={() => setIsCreateOpen(false)}
                className="p-1.5 rounded-lg text-charcoal-400 hover:text-charcoal-700 hover:bg-sand-100 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {createError && (
              <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-800 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{createError}</span>
              </div>
            )}

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Project Title <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Emerald Sky Villa, Contemporary Living Room"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
                <span className="text-[11px] text-charcoal-400 mt-1 block">
                  A unique web slug will be generated automatically.
                </span>
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Primary Category <span className="text-red-500">*</span>
                </label>
                <select
                  value={newCategory}
                  onChange={(e) => setNewCategory(e.target.value as ProjectCategory)}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-white text-charcoal-900"
                >
                  {CANONICAL_CATEGORIES.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Short Summary (Optional)
                </label>
                <textarea
                  rows={2}
                  maxLength={300}
                  placeholder="Brief 1-2 sentence synopsis for portfolio cards (min 10 chars to reach Ready status)..."
                  value={newShortDesc}
                  onChange={(e) => setNewShortDesc(e.target.value)}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-charcoal-800 mb-1">City</label>
                  <input
                    type="text"
                    placeholder="e.g. Bengaluru, Mumbai"
                    value={newCity}
                    onChange={(e) => setNewCity(e.target.value)}
                    className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-charcoal-800 mb-1">State</label>
                  <input
                    type="text"
                    placeholder="e.g. Karnataka, Maharashtra"
                    value={newState}
                    onChange={(e) => setNewState(e.target.value)}
                    className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
                  />
                </div>
              </div>

              <div className="pt-3 border-t border-sand-200 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsCreateOpen(false)}
                  className="px-4 py-2 rounded-xl border border-sand-300 text-charcoal-700 text-xs font-medium hover:bg-sand-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createLoading}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-charcoal-900 text-white text-xs font-medium hover:bg-charcoal-800 transition-colors disabled:opacity-50"
                >
                  {createLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Create & Edit Details</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
